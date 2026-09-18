package com.manuelperez.pipelinesentinel.domain.validation;

import java.util.List;

public record ValidationPreviewResult(
        String dataSourceCode,
        String schemaVersion,
        ValidationRunStatus status,
        ValidationOutcome outcome,
        IssueSeverity maxSeverity,
        ValidationCounts counts,
        List<ValidationIssue> issues
) {
}
