package com.mopl.domain.content.dto;

import com.mopl.domain.content.domain.ContentType;

import java.util.List;
import java.util.UUID;

/**
 * 콘텐츠 목록 조회 검색 파라미터
 *
 * GET /api/contents 스웨거 파라미터 기준
 */
public record ContentSearchRequest(
    ContentType typeEqual,
    String keywordLike,
    List<String> tagsIn,
    String cursor,
    UUID idAfter,
    int limit,
    String sortBy,
    String sortDirection
) {}