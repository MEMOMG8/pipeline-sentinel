package com.manuelperez.pipelinesentinel.domain.validation;

public record ValidationCounts(
        int totalRows,
        int validRows,
        int invalidRows,
        int issueCount
) {
}
