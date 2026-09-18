package com.manuelperez.pipelinesentinel.api.validation;

import com.manuelperez.pipelinesentinel.domain.validation.IssueCategory;
import com.manuelperez.pipelinesentinel.domain.validation.IssueSeverity;
import com.manuelperez.pipelinesentinel.domain.validation.ValidationIssue;

public record ValidationIssueResponse(
        Integer rowNumber,
        IssueCategory category,
        String code,
        String fieldName,
        IssueSeverity severity,
        String message
) {

    static ValidationIssueResponse from(ValidationIssue issue) {
        return new ValidationIssueResponse(
                issue.rowNumber(),
                issue.category(),
                issue.code(),
                issue.fieldName(),
                issue.severity(),
                issue.message()
        );
    }
}
