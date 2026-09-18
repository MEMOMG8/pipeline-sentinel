package com.manuelperez.pipelinesentinel.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String message = "No endpoint found for " + exception.getHttpMethod() + " " + exception.getRequestURL();
        return ResponseEntity.status(status).body(toErrorResponse(status, message, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(toErrorResponse(status, "Unexpected internal error", request));
    }

    private ApiErrorResponse toErrorResponse(HttpStatus status, String message, HttpServletRequest request) {
        return new ApiErrorResponse(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
    }
}
