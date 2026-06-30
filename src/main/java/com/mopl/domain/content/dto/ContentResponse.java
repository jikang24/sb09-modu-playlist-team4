package com.mopl.domain.content.dto;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ContentResponse(
    UUID id,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    BigDecimal averageRating,
    int reviewCount
    // TODO: watching_session 확정 후 추가
    // long watcherCount
) {
  public static ContentResponse from(Content content) {
    return new ContentResponse(
        content.getId(),
        content.getType(),
        content.getTitle(),
        content.getDescription(),
        content.getThumbnailUrl(),
        content.getTags(),
        content.getAverageRating(),
        content.getReviewCount()
    );
  }
}

