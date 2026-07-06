package com.mopl.global.dto;

import com.mopl.domain.content.domain.ContentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 다른 모듈이 콘텐츠 정보를 참조할 때 쓰는 공용 계약 타입 (UserSummary와 동일한 목적)
 *
 * Content 모듈의 ContentResponse를 직접 참조하지 않게 하기 위함
 */
public record ContentSummary(
    UUID id,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    BigDecimal averageRating,
    int reviewCount
) {}