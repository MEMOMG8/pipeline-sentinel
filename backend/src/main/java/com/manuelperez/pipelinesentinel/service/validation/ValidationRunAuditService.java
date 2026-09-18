package com.manuelperez.pipelinesentinel.service.validation;

import com.manuelperez.pipelinesentinel.persistence.validation.entity.QuarantinedRecordEntity;
import com.manuelperez.pipelinesentinel.persistence.validation.entity.ValidationIssueEntity;
import com.manuelperez.pipelinesentinel.persistence.validation.entity.ValidationRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ValidationRunAuditService {

    ValidationRunEntity submit(String dataSourceCode, String originalFilename, InputStream inputStream) throws IOException;

    Page<ValidationRunEntity> listRuns(Pageable pageable);

    ValidationRunEntity getRun(UUID runId);

    Page<ValidationIssueEntity> listIssues(UUID runId, Pageable pageable);

    Page<QuarantinedRecordEntity> listQuarantinedRecords(UUID runId, Pageable pageable);

    Map<UUID, List<String>> issueCodesByQuarantinedRecord(UUID runId, List<UUID> quarantinedRecordIds);
}
