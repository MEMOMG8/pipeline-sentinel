package com.manuelperez.pipelinesentinel.domain.validation;

import java.util.Map;

public record InvalidValidationRecord(
        int rowNumber,
        Map<String, String> rawRecord
) {
}
