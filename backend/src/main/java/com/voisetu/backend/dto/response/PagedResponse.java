package com.voisetu.backend.dto.response;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * @param <T> Content type
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {}
