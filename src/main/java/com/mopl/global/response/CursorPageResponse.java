package com.mopl.global.response;

import java.util.List;
import java.util.UUID;

public record CursorPageResponse<T>(
        List<T> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
) {}