package com.manuelperez.pipelinesentinel.api.validation;

import com.manuelperez.pipelinesentinel.domain.validation.IssueSeverity;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationOutcome;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationRunStatus;
import com.manuelperez.pipelinesentinel.persistence.validation.entity.ValidationRunEntity;

import java.time.Instant;
import java.util.UUID;

public record PersistedValidationRunResponse(
        UUID id,
        String dataSourceCode,
        String schemaVersion,
        String originalFilename,
        ValidationRunStatus status,
        ValidationOutcome outcome,
        IssueSeverity maxSeverity,
        ValidationCountsResponse counts,
        Instant submittedAt,
        Instant completedAt
) {

    static PersistedValidationRunResponse from(ValidationRunEntity run) {
        return new PersistedValidationRunResponse(
                run.getId(),
                run.getDataSource().getCode(),
                run.getDataSource().getSchemaVersion(),
                run.getOriginalFilename(),
                run.getStatus(),
                run.getOutcome(),
                run.getMaxSeverity(),
                new ValidationCountsResponse(
                        run.getTotalRows(),
                        run.getValidRows(),
                        run.getInvalidRows(),
                        run.getIssueCount()
                ),
                run.getSubmittedAt(),
                run.getCompletedAt()
        );
    }
}
