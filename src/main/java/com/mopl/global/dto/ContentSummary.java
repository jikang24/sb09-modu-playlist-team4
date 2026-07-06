package com.mopl.global.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ContentSummary(
    UUID id,
    String type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    BigDecimal averageRating,
    int reviewCount
) {

}
