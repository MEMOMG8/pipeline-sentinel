package com.manuelperez.pipelinesentinel.api.validation;

import com.manuelperez.pipelinesentinel.domain.validation.IssueCategory;
import com.manuelperez.pipelinesentinel.domain.validation.IssueSeverity;
import com.manuelperez.pipelinesentinel.persistence.validation.entity.ValidationIssueEntity;

public record ValidationIssueListItemResponse(
        Integer rowNumber,
        IssueCategory category,
        String code,
        String fieldName,
        IssueSeverity severity,
        String message
) {

    static ValidationIssueListItemResponse from(ValidationIssueEntity issue) {
        return new ValidationIssueListItemResponse(
                issue.getRowNumber(),
                issue.getCategory(),
                issue.getCode(),
                issue.getFieldName(),
                issue.getSeverity(),
                issue.getMessage()
        );
    }
}
