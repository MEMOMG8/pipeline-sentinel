package com.manuelperez.pipelinesentinel.api.validation;

import com.manuelperez.pipelinesentinel.persistence.validation.entity.QuarantinedRecordEntity;

import java.util.List;
import java.util.Map;

public record QuarantinedRecordResponse(
        int rowNumber,
        Map<String, String> rawRecord,
        List<String> issueCodes
) {

    static QuarantinedRecordResponse from(QuarantinedRecordEntity record, List<String> issueCodes) {
        return new QuarantinedRecordResponse(record.getRowNumber(), record.getRawRecord(), issueCodes);
    }
}
