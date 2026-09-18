package com.manuelperez.pipelinesentinel.domain.validation;

public record ValidationIssue(
        Integer rowNumber,
        IssueCategory category,
        String code,
        String fieldName,
        IssueSeverity severity,
        String message
) {
}
