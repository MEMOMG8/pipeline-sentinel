package com.manuelperez.pipelinesentinel.api.validation;

import com.manuelperez.pipelinesentinel.domain.validation.ValidationCounts;

public record ValidationCountsResponse(
        int totalRows,
        int validRows,
        int invalidRows,
        int issueCount
) {

    static ValidationCountsResponse from(ValidationCounts counts) {
        return new ValidationCountsResponse(
                counts.totalRows(),
                counts.validRows(),
                counts.invalidRows(),
                counts.issueCount()
        );
    }
}
