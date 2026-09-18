package com.manuelperez.pipelinesentinel.service.validation;

import com.manuelperez.pipelinesentinel.api.error.ApiBadRequestException;
import com.manuelperez.pipelinesentinel.domain.validation.InvalidValidationRecord;
import com.manuelperez.pipelinesentinel.domain.validation.IssueCategory;
import com.manuelperez.pipelinesentinel.domain.validation.IssueSeverity;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationCounts;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationIssue;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationOutcome;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationPreviewResult;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationRunStatus;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TransactionEventsValidationPreviewService implements ValidationPreviewService {

    private static final String DATA_SOURCE_CODE = "transaction-events";
    private static final String SCHEMA_VERSION = "v1";
    private static final List<String> REQUIRED_HEADERS = List.of(
            "transaction_id",
            "customer_id",
            "amount",
            "currency",
            "occurred_at",
            "schema_version"
    );
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");

    @Override
    public ValidationPreviewResult preview(
            String dataSourceCode,
            String originalFilename,
            InputStream inputStream
    ) throws IOException {
        if (!DATA_SOURCE_CODE.equals(dataSourceCode)) {
            throw new ApiBadRequestException("Unsupported dataSourceCode: " + dataSourceCode);
        }
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new ApiBadRequestException("Uploaded file must have a .csv extension");
        }

        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        if (content.trim().isEmpty()) {
            return rejected(0, 0, List.of(issue(
                    null,
                    IssueCategory.PROCESSING,
                    "EMPTY_FILE",
                    null,
                    IssueSeverity.HIGH,
                    "CSV file must not be empty"
            )));
        }

        List<CSVRecord> records;
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(false)
                .build()
                .parse(new StringReader(content))) {
            records = parser.getRecords();
        } catch (IllegalArgumentException exception) {
            return rejected(0, 0, List.of(issue(
                    null,
                    IssueCategory.PROCESSING,
                    "CSV_PARSE_ERROR",
                    null,
                    IssueSeverity.HIGH,
                    "CSV file could not be parsed"
            )));
        }

        if (records.isEmpty()) {
            return rejected(0, 0, List.of(issue(
                    null,
                    IssueCategory.PROCESSING,
                    "EMPTY_FILE",
                    null,
                    IssueSeverity.HIGH,
                    "CSV file must not be empty"
            )));
        }

        CSVRecord headerRecord = records.getFirst();
        int totalRows = Math.max(records.size() - 1, 0);
        HeaderValidation headerValidation = validateHeaders(headerRecord);
        if (!headerValidation.issues().isEmpty()) {
            return rejected(totalRows, totalRows, headerValidation.issues());
        }

        List<ValidationIssue> issues = new ArrayList<>();
        Set<String> transactionIds = new HashSet<>();
        Set<Long> invalidRecordNumbers = new HashSet<>();
        Map<Integer, InvalidValidationRecord> invalidRecords = new LinkedHashMap<>();

        for (int index = 1; index < records.size(); index++) {
            CSVRecord record = records.get(index);
            int issueCountBeforeRecord = issues.size();
            validateRequiredFields(record, headerValidation.headerIndexes(), issues);
            validateAmount(record, headerValidation.headerIndexes(), issues);
            validateCurrency(record, headerValidation.headerIndexes(), issues);
            validateOccurredAt(record, headerValidation.headerIndexes(), issues);
            validateSchemaVersion(record, headerValidation.headerIndexes(), issues);
            validateTransactionIdUniqueness(record, headerValidation.headerIndexes(), transactionIds, issues);
            if (issues.size() > issueCountBeforeRecord) {
                invalidRecordNumbers.add(record.getRecordNumber());
                invalidRecords.put(rowNumber(record), new InvalidValidationRecord(
                        rowNumber(record),
                        rawRecord(record, headerValidation.headerNames())
                ));
            }
        }

        int invalidRows = invalidRecordNumbers.size();
        int validRows = totalRows - invalidRows;
        if (issues.isEmpty()) {
            return passed(totalRows, validRows);
        }
        return rejected(totalRows, invalidRows, issues, new ArrayList<>(invalidRecords.values()));
    }

    private HeaderValidation validateHeaders(CSVRecord headerRecord) {
        Map<String, Integer> headerCounts = new HashMap<>();
        Map<String, Integer> headerIndexes = new HashMap<>();
        for (int index = 0; index < headerRecord.size(); index++) {
            String header = headerRecord.get(index);
            headerCounts.merge(header, 1, Integer::sum);
            headerIndexes.putIfAbsent(header, index);
        }

        List<ValidationIssue> issues = new ArrayList<>();
        for (String requiredHeader : REQUIRED_HEADERS) {
            int count = headerCounts.getOrDefault(requiredHeader, 0);
            if (count == 0) {
                issues.add(issue(
                        null,
                        IssueCategory.SCHEMA,
                        "MISSING_REQUIRED_HEADER",
                        requiredHeader,
                        IssueSeverity.HIGH,
                        "Required header is missing: " + requiredHeader
                ));
            } else if (count > 1) {
                issues.add(issue(
                        null,
                        IssueCategory.SCHEMA,
                        "DUPLICATE_REQUIRED_HEADER",
                        requiredHeader,
                        IssueSeverity.HIGH,
                        "Required header appears more than once: " + requiredHeader
                ));
            }
        }
        List<String> headerNames = new ArrayList<>();
        for (int index = 0; index < headerRecord.size(); index++) {
            headerNames.add(headerRecord.get(index));
        }
        return new HeaderValidation(headerIndexes, headerNames, issues);
    }

    private void validateRequiredFields(
            CSVRecord record,
            Map<String, Integer> headerIndexes,
            List<ValidationIssue> issues
    ) {
        for (String requiredHeader : REQUIRED_HEADERS) {
            if (fieldValue(record, headerIndexes, requiredHeader).isBlank()) {
                issues.add(issue(
                        rowNumber(record),
                        IssueCategory.REQUIRED_VALUE,
                        "REQUIRED_VALUE_MISSING",
                        requiredHeader,
                        IssueSeverity.MEDIUM,
                        "Required field must not be blank: " + requiredHeader
                ));
            }
        }
    }

    private void validateAmount(
            CSVRecord record,
            Map<String, Integer> headerIndexes,
            List<ValidationIssue> issues
    ) {
        String amountText = fieldValue(record, headerIndexes, "amount");
        if (amountText.isBlank()) {
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
        } catch (NumberFormatException exception) {
            issues.add(issue(
                    rowNumber(record),
                    IssueCategory.FORMAT,
                    "INVALID_AMOUNT_FORMAT",
                    "amount",
                    IssueSeverity.MEDIUM,
                    "amount must be a decimal number"
            ));
            return;
        }

        if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            issues.add(issue(
                    rowNumber(record),
                    IssueCategory.RANGE,
                    "AMOUNT_OUT_OF_RANGE",
                    "amount",
                    IssueSeverity.MEDIUM,
                    "amount must be between 0.01 and 100000.00"
            ));
        }
    }

    private void validateCurrency(
            CSVRecord record,
            Map<String, Integer> headerIndexes,
            List<ValidationIssue> issues
    ) {
        String currency = fieldValue(record, headerIndexes, "currency");
        if (!currency.isBlank() && !"USD".equals(currency)) {
            issues.add(issue(
                    rowNumber(record),
                    IssueCategory.FORMAT,
                    "UNSUPPORTED_CURRENCY",
                    "currency",
                    IssueSeverity.MEDIUM,
                    "currency must equal USD"
            ));
        }
    }

    private void validateOccurredAt(
            CSVRecord record,
            Map<String, Integer> headerIndexes,
            List<ValidationIssue> issues
    ) {
        String occurredAt = fieldValue(record, headerIndexes, "occurred_at");
        if (occurredAt.isBlank()) {
            return;
        }

        try {
            OffsetDateTime timestamp = OffsetDateTime.parse(occurredAt);
            if (timestamp.getOffset().getTotalSeconds() != 0) {
                addInvalidOccurredAtIssue(record, issues);
            }
        } catch (DateTimeParseException exception) {
            addInvalidOccurredAtIssue(record, issues);
        }
    }

    private void validateSchemaVersion(
            CSVRecord record,
            Map<String, Integer> headerIndexes,
            List<ValidationIssue> issues
    ) {
        String schemaVersion = fieldValue(record, headerIndexes, "schema_version");
        if (!schemaVersion.isBlank() && !SCHEMA_VERSION.equals(schemaVersion)) {
            issues.add(issue(
                    rowNumber(record),
                    IssueCategory.SCHEMA,
                    "UNSUPPORTED_SCHEMA_VERSION",
                    "schema_version",
                    IssueSeverity.HIGH,
                    "schema_version must equal v1"
            ));
        }
    }

    private void validateTransactionIdUniqueness(
            CSVRecord record,
            Map<String, Integer> headerIndexes,
            Set<String> transactionIds,
            List<ValidationIssue> issues
    ) {
        String transactionId = fieldValue(record, headerIndexes, "transaction_id");
        if (transactionId.isBlank()) {
            return;
        }
        if (!transactionIds.add(transactionId)) {
            issues.add(issue(
                    rowNumber(record),
                    IssueCategory.DUPLICATE,
                    "DUPLICATE_TRANSACTION_ID",
                    "transaction_id",
                    IssueSeverity.HIGH,
                    "transaction_id must be unique within the file"
            ));
        }
    }

    private void addInvalidOccurredAtIssue(CSVRecord record, List<ValidationIssue> issues) {
        issues.add(issue(
                rowNumber(record),
                IssueCategory.FORMAT,
                "INVALID_OCCURRED_AT",
                "occurred_at",
                IssueSeverity.MEDIUM,
                "occurred_at must be a valid ISO-8601 timestamp with zero UTC offset"
        ));
    }

    private String fieldValue(CSVRecord record, Map<String, Integer> headerIndexes, String fieldName) {
        int index = headerIndexes.get(fieldName);
        if (index >= record.size()) {
            return "";
        }
        return record.get(index).trim();
    }

    private Integer rowNumber(CSVRecord record) {
        return Math.toIntExact(record.getRecordNumber());
    }

    private Map<String, String> rawRecord(CSVRecord record, List<String> headerNames) {
        Map<String, String> rawRecord = new LinkedHashMap<>();
        for (int index = 0; index < headerNames.size(); index++) {
            String value = index < record.size() ? record.get(index) : "";
            rawRecord.put(headerNames.get(index), value);
        }
        return rawRecord;
    }

    private ValidationPreviewResult passed(int totalRows, int validRows) {
        return new ValidationPreviewResult(
                DATA_SOURCE_CODE,
                SCHEMA_VERSION,
                ValidationRunStatus.COMPLETED,
                ValidationOutcome.PASSED,
                null,
                new ValidationCounts(totalRows, validRows, 0, 0),
                List.of(),
                List.of()
        );
    }

    private ValidationPreviewResult rejected(int totalRows, int invalidRows, List<ValidationIssue> issues) {
        return rejected(totalRows, invalidRows, issues, List.of());
    }

    private ValidationPreviewResult rejected(
            int totalRows,
            int invalidRows,
            List<ValidationIssue> issues,
            List<InvalidValidationRecord> invalidRecords
    ) {
        int validRows = Math.max(totalRows - invalidRows, 0);
        return new ValidationPreviewResult(
                DATA_SOURCE_CODE,
                SCHEMA_VERSION,
                ValidationRunStatus.COMPLETED,
                ValidationOutcome.REJECTED,
                maxSeverity(issues),
                new ValidationCounts(totalRows, validRows, invalidRows, issues.size()),
                List.copyOf(issues),
                List.copyOf(invalidRecords)
        );
    }

    private IssueSeverity maxSeverity(List<ValidationIssue> issues) {
        if (issues.stream().anyMatch(issue -> issue.severity() == IssueSeverity.HIGH)) {
            return IssueSeverity.HIGH;
        }
        if (issues.stream().anyMatch(issue -> issue.severity() == IssueSeverity.MEDIUM)) {
            return IssueSeverity.MEDIUM;
        }
        return null;
    }

    private ValidationIssue issue(
            Integer rowNumber,
            IssueCategory category,
            String code,
            String fieldName,
            IssueSeverity severity,
            String message
    ) {
        return new ValidationIssue(rowNumber, category, code, fieldName, severity, message);
    }

    private record HeaderValidation(
            Map<String, Integer> headerIndexes,
            List<String> headerNames,
            List<ValidationIssue> issues
    ) {
    }
}
