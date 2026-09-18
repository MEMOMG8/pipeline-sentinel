package com.manuelperez.pipelinesentinel.api.validation;

import com.manuelperez.pipelinesentinel.domain.validation.IssueSeverity;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationOutcome;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationPreviewResult;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationRunStatus;

import java.util.List;

public record ValidationPreviewResponse(
        String dataSourceCode,
        String schemaVersion,
        ValidationRunStatus status,
        ValidationOutcome outcome,
        IssueSeverity maxSeverity,
        ValidationCountsResponse counts,
        List<ValidationIssueResponse> issues
) {

    static ValidationPreviewResponse from(ValidationPreviewResult result) {
        return new ValidationPreviewResponse(
                result.dataSourceCode(),
                result.schemaVersion(),
                result.status(),
                result.outcome(),
                result.maxSeverity(),
                ValidationCountsResponse.from(result.counts()),
                result.issues().stream().map(ValidationIssueResponse::from).toList()
        );
    }
}
