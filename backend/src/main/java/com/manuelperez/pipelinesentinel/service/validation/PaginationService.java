package com.manuelperez.pipelinesentinel.service.validation;

import com.manuelperez.pipelinesentinel.api.error.ApiBadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PaginationService {

    private static final int MAX_PAGE_SIZE = 100;

    private PaginationService() {
    }

    public static Pageable pageRequest(int page, int size) {
        if (page < 0) {
            throw new ApiBadRequestException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiBadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size);
    }
}
