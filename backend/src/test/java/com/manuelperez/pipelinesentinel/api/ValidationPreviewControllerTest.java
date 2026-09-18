package com.manuelperez.pipelinesentinel.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationPreviewControllerTest {

    private static final String ENDPOINT = "/api/v1/validation-runs/preview";
    private static final String VALID_CSV = """
            transaction_id,customer_id,amount,currency,occurred_at,schema_version
            txn-001,cust-001,12.34,USD,2026-01-01T00:00:00Z,v1
            txn-002,cust-002,56.78,USD,2026-01-02T00:00:00Z,v1
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validMultipartPreviewRequestReturnsPassedPreview() throws Exception {
        mockMvc.perform(multipart(ENDPOINT)
                        .file(csvFile("events.csv", VALID_CSV))
                        .param("dataSourceCode", "transaction-events"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dataSourceCode").value("transaction-events"))
                .andExpect(jsonPath("$.schemaVersion").value("v1"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.outcome").value("PASSED"))
                .andExpect(jsonPath("$.maxSeverity").doesNotExist())
                .andExpect(jsonPath("$.counts.totalRows").value(2))
                .andExpect(jsonPath("$.counts.validRows").value(2))
                .andExpect(jsonPath("$.counts.invalidRows").value(0))
                .andExpect(jsonPath("$.counts.issueCount").value(0))
                .andExpect(jsonPath("$.issues").isEmpty());
    }

    @Test
    void unknownDataSourceReturnsBadRequestErrorFormat() throws Exception {
        mockMvc.perform(multipart(ENDPOINT)
                        .file(csvFile("events.csv", VALID_CSV))
                        .param("dataSourceCode", "unknown-source"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Unsupported dataSourceCode: unknown-source"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
    }

    @Test
    void nonCsvFilenameReturnsBadRequestErrorFormat() throws Exception {
        mockMvc.perform(multipart(ENDPOINT)
                        .file(csvFile("events.txt", VALID_CSV))
                        .param("dataSourceCode", "transaction-events"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Uploaded file must have a .csv extension"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
    }

    private MockMultipartFile csvFile(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
