package com.manuelperez.pipelinesentinel.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manuelperez.pipelinesentinel.persistence.validation.repository.DataSourceRepository;
import com.manuelperez.pipelinesentinel.persistence.validation.repository.QuarantinedRecordRepository;
import com.manuelperez.pipelinesentinel.persistence.validation.repository.ValidationIssueRepository;
import com.manuelperez.pipelinesentinel.persistence.validation.repository.ValidationRunRepository;
import com.manuelperez.pipelinesentinel.service.validation.ValidationRunAuditService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testcontainers")
@Testcontainers(disabledWithoutDocker = true)
class ValidationRunPersistenceIntegrationTest {

    private static final String SUBMIT_ENDPOINT = "/api/v1/validation-runs";
    private static final String PREVIEW_ENDPOINT = "/api/v1/validation-runs/preview";
    private static final String VALID_CSV = """
            transaction_id,customer_id,amount,currency,occurred_at,schema_version
            txn-001,cust-001,12.34,USD,2026-01-01T00:00:00Z,v1
            txn-002,cust-002,56.78,USD,2026-01-02T00:00:00Z,v1
            """;
    private static final String INVALID_CSV = """
            transaction_id,customer_id,amount,currency,occurred_at,schema_version
            txn-001,cust-001,12.34,USD,2026-01-01T00:00:00Z,v1
            txn-001,cust-002,0.00,EUR,2026-01-02T00:00:00-05:00,v2
            """;

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private ValidationRunRepository validationRunRepository;

    @Autowired
    private ValidationIssueRepository validationIssueRepository;

    @Autowired
    private QuarantinedRecordRepository quarantinedRecordRepository;

    @Autowired
    private ValidationRunAuditService validationRunAuditService;

    @Test
    void flywayMigrationAppliesAndSeededDataSourceExists() {
        assertThat(validationRunAuditService).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(dataSourceRepository.findByCodeAndActiveTrue("transaction-events"))
                .hasValueSatisfying(dataSource -> {
                    assertThat(dataSource.getDisplayName()).isEqualTo("Transaction Events");
                    assertThat(dataSource.getSchemaVersion()).isEqualTo("v1");
                });
    }

    @Test
    void persistedSubmissionEndpointsRoundTripValidRun() throws Exception {
        UUID runId = submit("valid-events.csv", VALID_CSV)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataSourceCode").value("transaction-events"))
                .andExpect(jsonPath("$.originalFilename").value("valid-events.csv"))
                .andExpect(jsonPath("$.outcome").value("PASSED"))
                .andExpect(jsonPath("$.counts.totalRows").value(2))
                .andReturnId();

        assertThat(validationRunRepository.findById(runId)).isPresent();

        mockMvc.perform(get(SUBMIT_ENDPOINT).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(runId.toString()));

        mockMvc.perform(get(SUBMIT_ENDPOINT + "/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(runId.toString()))
                .andExpect(jsonPath("$.outcome").value("PASSED"));
    }

    @Test
    void invalidSubmissionPersistsIssuesAndOneQuarantinedRecordForMultiIssueRow() throws Exception {
        UUID runId = submit("invalid-events.csv", INVALID_CSV)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("REJECTED"))
                .andExpect(jsonPath("$.maxSeverity").value("HIGH"))
                .andExpect(jsonPath("$.counts.totalRows").value(2))
                .andExpect(jsonPath("$.counts.validRows").value(1))
                .andExpect(jsonPath("$.counts.invalidRows").value(1))
                .andExpect(jsonPath("$.counts.issueCount").value(5))
                .andReturnId();

        assertThat(validationIssueRepository.findByValidationRun_IdOrderByRowNumberAscCreatedAtAsc(
                runId,
                org.springframework.data.domain.PageRequest.of(0, 20)
        ).getTotalElements()).isEqualTo(5);
        assertThat(quarantinedRecordRepository.findByValidationRun_Id(runId)).hasSize(1);

        mockMvc.perform(get(SUBMIT_ENDPOINT + "/" + runId + "/issues").param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content[0].rowNumber").value(3))
                .andExpect(jsonPath("$.content[0].code").value("AMOUNT_OUT_OF_RANGE"));

        mockMvc.perform(get(SUBMIT_ENDPOINT + "/" + runId + "/quarantined-records").param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rowNumber").value(3))
                .andExpect(jsonPath("$.content[0].rawRecord.transaction_id").value("txn-001"))
                .andExpect(jsonPath("$.content[0].rawRecord.currency").value("EUR"))
                .andExpect(jsonPath("$.content[0].issueCodes.length()").value(5));
    }

    @Test
    void unknownRunIdReturnsStandardNotFoundError() throws Exception {
        UUID unknownRunId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(get(SUBMIT_ENDPOINT + "/" + unknownRunId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Validation run not found: " + unknownRunId))
                .andExpect(jsonPath("$.path").value(SUBMIT_ENDPOINT + "/" + unknownRunId));
    }

    @Test
    void previewEndpointStillDoesNotPersistRun() throws Exception {
        long countBefore = validationRunRepository.count();

        mockMvc.perform(multipart(PREVIEW_ENDPOINT)
                        .file(csvFile("preview-events.csv", VALID_CSV))
                        .param("dataSourceCode", "transaction-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("PASSED"));

        assertThat(validationRunRepository.count()).isEqualTo(countBefore);
    }

    private SubmitResultActions submit(String filename, String csv) throws Exception {
        return new SubmitResultActions(mockMvc.perform(multipart(SUBMIT_ENDPOINT)
                .file(csvFile(filename, csv))
                .param("dataSourceCode", "transaction-events")));
    }

    private MockMultipartFile csvFile(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private class SubmitResultActions {

        private final org.springframework.test.web.servlet.ResultActions resultActions;

        private SubmitResultActions(org.springframework.test.web.servlet.ResultActions resultActions) {
            this.resultActions = resultActions;
        }

        private SubmitResultActions andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            resultActions.andExpect(matcher);
            return this;
        }

        private UUID andReturnId() throws Exception {
            JsonNode response = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString());
            return UUID.fromString(response.get("id").asText());
        }
    }
}
