package com.manuelperez.pipelinesentinel.api.validation;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PagedResponse<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<T> content
) {

    static <S, T> PagedResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PagedResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent().stream().map(mapper).toList()
        );
    }
}
