package com.manuelperez.pipelinesentinel.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiBadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            ApiBadRequestException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(toErrorResponse(status, exception.getMessage(), request));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Required multipart part is missing: " + exception.getRequestPartName();
        return ResponseEntity.status(status).body(toErrorResponse(status, message, request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Required request parameter is missing: " + exception.getParameterName();
        return ResponseEntity.status(status).body(toErrorResponse(status, message, request));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipartException(HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(toErrorResponse(status, "Invalid multipart request", request));
    }

    @ExceptionHandler(ApiNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ApiNotFoundException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(toErrorResponse(status, exception.getMessage(), request));
    }

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
