package com.manuelperez.pipelinesentinel.api.validation;

import com.manuelperez.pipelinesentinel.persistence.validation.entity.QuarantinedRecordEntity;
import com.manuelperez.pipelinesentinel.persistence.validation.entity.ValidationRunEntity;
import com.manuelperez.pipelinesentinel.service.validation.PaginationService;
import com.manuelperez.pipelinesentinel.service.validation.ValidationRunAuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/validation-runs")
public class ValidationRunController {

    private final ValidationRunAuditService validationRunService;

    public ValidationRunController(ValidationRunAuditService validationRunService) {
        this.validationRunService = validationRunService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PersistedValidationRunResponse> submit(
            @RequestPart("file") MultipartFile file,
            @RequestParam("dataSourceCode") String dataSourceCode
    ) throws IOException {
        ValidationRunEntity run = validationRunService.submit(
                dataSourceCode,
                file.getOriginalFilename(),
                file.getInputStream()
        );
        return ResponseEntity.created(URI.create("/api/v1/validation-runs/" + run.getId()))
                .body(PersistedValidationRunResponse.from(run));
    }

    @GetMapping
    public PagedResponse<PersistedValidationRunResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PaginationService.pageRequest(page, size);
        return PagedResponse.from(validationRunService.listRuns(pageable), PersistedValidationRunResponse::from);
    }

    @GetMapping("/{runId}")
    public PersistedValidationRunResponse detail(@PathVariable UUID runId) {
        return PersistedValidationRunResponse.from(validationRunService.getRun(runId));
    }

    @GetMapping("/{runId}/issues")
    public PagedResponse<ValidationIssueListItemResponse> issues(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PaginationService.pageRequest(page, size);
        return PagedResponse.from(validationRunService.listIssues(runId, pageable), ValidationIssueListItemResponse::from);
    }

    @GetMapping("/{runId}/quarantined-records")
    public PagedResponse<QuarantinedRecordResponse> quarantinedRecords(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PaginationService.pageRequest(page, size);
        Page<QuarantinedRecordEntity> records = validationRunService.listQuarantinedRecords(runId, pageable);
        List<UUID> recordIds = records.getContent().stream().map(QuarantinedRecordEntity::getId).toList();
        Map<UUID, List<String>> issueCodes = validationRunService.issueCodesByQuarantinedRecord(runId, recordIds);
        return PagedResponse.from(records, record -> QuarantinedRecordResponse.from(
                record,
                issueCodes.getOrDefault(record.getId(), List.of())
        ));
    }
}
