package com.manuelperez.pipelinesentinel.api.error;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path
) {
}
