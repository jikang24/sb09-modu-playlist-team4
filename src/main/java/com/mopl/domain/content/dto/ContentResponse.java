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
    int reviewCount,
    long watcherCount
) {
  public static ContentResponse from(Content content, long watcherCount, String thumbnailUrl) {
    return new ContentResponse(
        content.getId(),
        content.getType(),
        content.getTitle(),
        content.getDescription(),
        thumbnailUrl,
        content.getTags(),
        content.getAverageRating(),
        content.getReviewCount(),
        watcherCount
    );
  }
}

