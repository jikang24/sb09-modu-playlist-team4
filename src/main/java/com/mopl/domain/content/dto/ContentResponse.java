package com.mopl.domain.content.dto;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ContentResponse(
    UUID id,
    ContentType type,
    String externalId,
    String title,
    String description,
    String thumbnailUrl,
    BigDecimal averageRating,
    int reviewCount,
    List<String> tags,
    Instant createdAt,
    Instant updatedAt
) {
  public static ContentResponse from(Content content) {
    return new ContentResponse(
        content.getId(),
        content.getType(),
        content.getExternalId(),
        content.getTitle(),
        content.getDescription(),
        content.getThumbnailUrl(),
        content.getAverageRating(),
        content.getReviewCount(),
        content.getTags().stream()
            .map(tag -> tag.getTag())
            .toList(),
        content.getCreatedAt(),
        content.getUpdatedAt()
    );
  }

}
