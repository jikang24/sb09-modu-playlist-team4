package com.mopl.domain.review.dto;

import java.util.UUID;

//리뷰 목록 조회 검색 파라미터
public record ReviewSearchRequest(
    UUID contentId,
    String cursor,
    UUID idAfter,
    int limit,
    String sortBy,
    String sortDirection
) {}