package com.mopl.domain.watchingsession.dto;

import java.util.UUID;

// 콘텐츠별 시청 세션 목록 조회 검색 파라미터
public record WatchingSessionSearchRequest(
    UUID contentId,
    String cursor,
    UUID idAfter,
    int limit,
    String sortBy,
    String sortDirection
) {}