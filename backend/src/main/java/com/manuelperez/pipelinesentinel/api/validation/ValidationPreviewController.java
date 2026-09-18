package com.manuelperez.pipelinesentinel.api.validation;

import com.manuelperez.pipelinesentinel.domain.validation.ValidationPreviewResult;
import com.manuelperez.pipelinesentinel.service.validation.ValidationPreviewService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class ValidationPreviewController {

    private final ValidationPreviewService validationPreviewService;

    public ValidationPreviewController(ValidationPreviewService validationPreviewService) {
        this.validationPreviewService = validationPreviewService;
    }

    @PostMapping(value = "/validation-runs/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValidationPreviewResponse preview(
            @RequestPart("file") MultipartFile file,
            @RequestParam("dataSourceCode") String dataSourceCode
    ) throws IOException {
        ValidationPreviewResult result = validationPreviewService.preview(
                dataSourceCode,
                file.getOriginalFilename(),
                file.getInputStream()
        );
        return ValidationPreviewResponse.from(result);
    }
}
