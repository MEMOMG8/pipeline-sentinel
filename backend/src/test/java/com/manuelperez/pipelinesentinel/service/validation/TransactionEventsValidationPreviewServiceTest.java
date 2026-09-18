package com.manuelperez.pipelinesentinel.service.validation;

import com.manuelperez.pipelinesentinel.domain.validation.IssueCategory;
import com.manuelperez.pipelinesentinel.domain.validation.IssueSeverity;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationOutcome;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationPreviewResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEventsValidationPreviewServiceTest {

    private final TransactionEventsValidationPreviewService service = new TransactionEventsValidationPreviewService();

    @Test
    void validCsvPasses() throws Exception {
        ValidationPreviewResult result = preview(validCsv());

        assertThat(result.outcome()).isEqualTo(ValidationOutcome.PASSED);
        assertThat(result.maxSeverity()).isNull();
        assertThat(result.counts().totalRows()).isEqualTo(2);
        assertThat(result.counts().validRows()).isEqualTo(2);
        assertThat(result.counts().invalidRows()).isZero();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void missingRequiredHeaderProducesSchemaIssue() throws Exception {
        String csv = """
                transaction_id,customer_id,amount,currency,occurred_at
                txn-001,cust-001,12.34,USD,2026-01-01T00:00:00Z
                """;

        ValidationPreviewResult result = preview(csv);

        assertSingleIssue(result, null, IssueCategory.SCHEMA, "MISSING_REQUIRED_HEADER", "schema_version", IssueSeverity.HIGH);
    }

    @Test
    void blankRequiredValueProducesRowLevelIssue() throws Exception {
        String csv = """
                transaction_id,customer_id,amount,currency,occurred_at,schema_version
                txn-001,   ,12.34,USD,2026-01-01T00:00:00Z,v1
                """;

        ValidationPreviewResult result = preview(csv);

        assertSingleIssue(result, 2, IssueCategory.REQUIRED_VALUE, "REQUIRED_VALUE_MISSING", "customer_id", IssueSeverity.MEDIUM);
    }

    @Test
    void nonnumericAmountProducesFormatIssue() throws Exception {
        String csv = """
                transaction_id,customer_id,amount,currency,occurred_at,schema_version
                txn-001,cust-001,not-a-number,USD,2026-01-01T00:00:00Z,v1
                """;

        ValidationPreviewResult result = preview(csv);

        assertSingleIssue(result, 2, IssueCategory.FORMAT, "INVALID_AMOUNT_FORMAT", "amount", IssueSeverity.MEDIUM);
    }

    @Test
    void outOfRangeAmountProducesRangeIssue() throws Exception {
        String csv = """
                transaction_id,customer_id,amount,currency,occurred_at,schema_version
                txn-001,cust-001,100000.01,USD,2026-01-01T00:00:00Z,v1
                """;

        ValidationPreviewResult result = preview(csv);

        assertSingleIssue(result, 2, IssueCategory.RANGE, "AMOUNT_OUT_OF_RANGE", "amount", IssueSeverity.MEDIUM);
    }

    @Test
    void nonUsdCurrencyIsRejected() throws Exception {
        String csv = """
                transaction_id,customer_id,amount,currency,occurred_at,schema_version
                txn-001,cust-001,12.34,EUR,2026-01-01T00:00:00Z,v1
                """;

        ValidationPreviewResult result = preview(csv);

        assertSingleIssue(result, 2, IssueCategory.FORMAT, "UNSUPPORTED_CURRENCY", "currency", IssueSeverity.MEDIUM);
    }

    @Test
    void invalidOrNonUtcTimestampIsRejected() throws Exception {
        String csv = """
                transaction_id,customer_id,amount,currency,occurred_at,schema_version
                txn-001,cust-001,12.34,USD,2026-01-01T00:00:00-05:00,v1
                """;

        ValidationPreviewResult result = preview(csv);

        assertSingleIssue(result, 2, IssueCategory.FORMAT, "INVALID_OCCURRED_AT", "occurred_at", IssueSeverity.MEDIUM);
    }

    @Test
    void unsupportedSchemaVersionIsRejected() throws Exception {
        String csv = """
                transaction_id,customer_id,amount,currency,occurred_at,schema_version
                txn-001,cust-001,12.34,USD,2026-01-01T00:00:00Z,v2
                """;

        ValidationPreviewResult result = preview(csv);

        assertSingleIssue(result, 2, IssueCategory.SCHEMA, "UNSUPPORTED_SCHEMA_VERSION", "schema_version", IssueSeverity.HIGH);
    }

    @Test
    void duplicateTransactionIdRejectsOnlyLaterDuplicateRows() throws Exception {
        String csv = """
                transaction_id,customer_id,amount,currency,occurred_at,schema_version
                txn-001,cust-001,12.34,USD,2026-01-01T00:00:00Z,v1
                txn-002,cust-002,56.78,USD,2026-01-02T00:00:00Z,v1
                txn-001,cust-003,90.12,USD,2026-01-03T00:00:00Z,v1
                """;

        ValidationPreviewResult result = preview(csv);

        assertThat(result.outcome()).isEqualTo(ValidationOutcome.REJECTED);
        assertThat(result.counts().totalRows()).isEqualTo(3);
        assertThat(result.counts().validRows()).isEqualTo(2);
        assertThat(result.counts().invalidRows()).isEqualTo(1);
        assertSingleIssue(result, 4, IssueCategory.DUPLICATE, "DUPLICATE_TRANSACTION_ID", "transaction_id", IssueSeverity.HIGH);
    }

    private ValidationPreviewResult preview(String csv) throws Exception {
        return service.preview(
                "transaction-events",
                "events.csv",
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );
    }

    private String validCsv() {
        return """
                transaction_id,customer_id,amount,currency,occurred_at,schema_version
                txn-001,cust-001,12.34,USD,2026-01-01T00:00:00Z,v1
                txn-002,cust-002,56.78,USD,2026-01-02T00:00:00Z,v1
                """;
    }

    private void assertSingleIssue(
            ValidationPreviewResult result,
            Integer rowNumber,
            IssueCategory category,
            String code,
            String fieldName,
            IssueSeverity severity
    ) {
        assertThat(result.outcome()).isEqualTo(ValidationOutcome.REJECTED);
        assertThat(result.issues()).hasSize(1);
        assertThat(result.issues().getFirst().rowNumber()).isEqualTo(rowNumber);
        assertThat(result.issues().getFirst().category()).isEqualTo(category);
        assertThat(result.issues().getFirst().code()).isEqualTo(code);
        assertThat(result.issues().getFirst().fieldName()).isEqualTo(fieldName);
        assertThat(result.issues().getFirst().severity()).isEqualTo(severity);
    }
}
