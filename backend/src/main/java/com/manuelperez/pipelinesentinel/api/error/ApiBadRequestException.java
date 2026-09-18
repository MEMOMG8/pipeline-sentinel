package com.manuelperez.pipelinesentinel.api.error;

public class ApiBadRequestException extends RuntimeException {

    public ApiBadRequestException(String message) {
        super(message);
    }
}
