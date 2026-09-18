package com.manuelperez.pipelinesentinel.service.validation;

import com.manuelperez.pipelinesentinel.domain.validation.ValidationPreviewResult;

import java.io.IOException;
import java.io.InputStream;

public interface ValidationPreviewService {

    ValidationPreviewResult preview(
            String dataSourceCode,
            String originalFilename,
            InputStream inputStream
    ) throws IOException;
}
