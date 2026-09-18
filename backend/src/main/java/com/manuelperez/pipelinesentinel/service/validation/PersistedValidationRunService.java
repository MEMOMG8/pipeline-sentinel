package com.manuelperez.pipelinesentinel.service.validation;

import com.manuelperez.pipelinesentinel.api.error.ApiBadRequestException;
import com.manuelperez.pipelinesentinel.api.error.ApiNotFoundException;
import com.manuelperez.pipelinesentinel.domain.validation.InvalidValidationRecord;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationIssue;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationPreviewResult;
import com.manuelperez.pipelinesentinel.persistence.validation.entity.DataSourceEntity;
import com.manuelperez.pipelinesentinel.persistence.validation.entity.QuarantinedRecordEntity;
import com.manuelperez.pipelinesentinel.persistence.validation.entity.ValidationIssueEntity;
import com.manuelperez.pipelinesentinel.persistence.validation.entity.ValidationRunEntity;
import com.manuelperez.pipelinesentinel.persistence.validation.repository.DataSourceRepository;
import com.manuelperez.pipelinesentinel.persistence.validation.repository.QuarantinedRecordRepository;
import com.manuelperez.pipelinesentinel.persistence.validation.repository.ValidationIssueRepository;
import com.manuelperez.pipelinesentinel.persistence.validation.repository.ValidationRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PersistedValidationRunService implements ValidationRunAuditService {

    private final ValidationPreviewService validationPreviewService;
    private final DataSourceRepository dataSourceRepository;
    private final ValidationRunRepository validationRunRepository;
    private final QuarantinedRecordRepository quarantinedRecordRepository;
    private final ValidationIssueRepository validationIssueRepository;

    public PersistedValidationRunService(
            ValidationPreviewService validationPreviewService,
            DataSourceRepository dataSourceRepository,
            ValidationRunRepository validationRunRepository,
            QuarantinedRecordRepository quarantinedRecordRepository,
            ValidationIssueRepository validationIssueRepository
    ) {
        this.validationPreviewService = validationPreviewService;
        this.dataSourceRepository = dataSourceRepository;
        this.validationRunRepository = validationRunRepository;
        this.quarantinedRecordRepository = quarantinedRecordRepository;
        this.validationIssueRepository = validationIssueRepository;
    }

    @Transactional
    @Override
    public ValidationRunEntity submit(
            String dataSourceCode,
            String originalFilename,
            InputStream inputStream
    ) throws IOException {
        Instant submittedAt = Instant.now();
        ValidationPreviewResult result = validationPreviewService.preview(dataSourceCode, originalFilename, inputStream);
        DataSourceEntity dataSource = dataSourceRepository.findByCodeAndActiveTrue(dataSourceCode)
                .filter(source -> source.getSchemaVersion().equals(result.schemaVersion()))
                .orElseThrow(() -> new ApiBadRequestException("Unsupported dataSourceCode: " + dataSourceCode));

        Instant completedAt = Instant.now();
        ValidationRunEntity run = validationRunRepository.save(new ValidationRunEntity(
                dataSource,
                originalFilename,
                result.status(),
                result.outcome(),
                result.maxSeverity(),
                result.counts().totalRows(),
                result.counts().validRows(),
                result.counts().invalidRows(),
                result.counts().issueCount(),
                submittedAt,
                completedAt
        ));

        Map<Integer, QuarantinedRecordEntity> recordsByRowNumber = persistQuarantinedRecords(run, result.invalidRecords(), completedAt);
        persistIssues(run, result.issues(), recordsByRowNumber, completedAt);
        return run;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ValidationRunEntity> listRuns(Pageable pageable) {
        return validationRunRepository.findAllByOrderBySubmittedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public ValidationRunEntity getRun(UUID runId) {
        return validationRunRepository.findById(runId)
                .orElseThrow(() -> new ApiNotFoundException("Validation run not found: " + runId));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ValidationIssueEntity> listIssues(UUID runId, Pageable pageable) {
        getRun(runId);
        return validationIssueRepository.findByValidationRun_IdOrderByRowNumberAscCreatedAtAsc(runId, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<QuarantinedRecordEntity> listQuarantinedRecords(UUID runId, Pageable pageable) {
        getRun(runId);
        return quarantinedRecordRepository.findByValidationRun_IdOrderByRowNumberAsc(runId, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Map<UUID, List<String>> issueCodesByQuarantinedRecord(UUID runId, List<UUID> quarantinedRecordIds) {
        if (quarantinedRecordIds.isEmpty()) {
            return Map.of();
        }
        List<ValidationIssueEntity> issues = validationIssueRepository.findByValidationRun_IdAndQuarantinedRecord_IdIn(
                runId,
                quarantinedRecordIds
        );
        Map<UUID, List<String>> issueCodes = new HashMap<>();
        for (ValidationIssueEntity issue : issues) {
            UUID recordId = issue.getQuarantinedRecord().getId();
            issueCodes.computeIfAbsent(recordId, ignored -> new java.util.ArrayList<>()).add(issue.getCode());
        }
        return issueCodes;
    }

    private Map<Integer, QuarantinedRecordEntity> persistQuarantinedRecords(
            ValidationRunEntity run,
            List<InvalidValidationRecord> invalidRecords,
            Instant createdAt
    ) {
        Map<Integer, QuarantinedRecordEntity> recordsByRowNumber = new HashMap<>();
        for (InvalidValidationRecord invalidRecord : invalidRecords) {
            QuarantinedRecordEntity entity = quarantinedRecordRepository.save(new QuarantinedRecordEntity(
                    run,
                    invalidRecord.rowNumber(),
                    invalidRecord.rawRecord(),
                    createdAt
            ));
            recordsByRowNumber.put(entity.getRowNumber(), entity);
        }
        return recordsByRowNumber;
    }

    private void persistIssues(
            ValidationRunEntity run,
            List<ValidationIssue> issues,
            Map<Integer, QuarantinedRecordEntity> recordsByRowNumber,
            Instant createdAt
    ) {
        for (ValidationIssue issue : issues) {
            QuarantinedRecordEntity quarantinedRecord = issue.rowNumber() == null
                    ? null
                    : recordsByRowNumber.get(issue.rowNumber());
            validationIssueRepository.save(new ValidationIssueEntity(
                    run,
                    quarantinedRecord,
                    issue.rowNumber(),
                    issue.category(),
                    issue.code(),
                    issue.fieldName(),
                    issue.severity(),
                    issue.message(),
                    createdAt
            ));
        }
    }
}
