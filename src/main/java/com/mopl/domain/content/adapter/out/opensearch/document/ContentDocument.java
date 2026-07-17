package com.mopl.domain.content.adapter.out.opensearch.document;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDocument {

  private UUID id;

  private String title;

  private String description;

  private List<String> tags;

  private ContentType type;

  private BigDecimal averageRating;

  private Integer reviewCount;

  // OpenSearch 전용 DTO이므로 Jackson이 기본 지원하지 않는 java.time.Instant 대신
  // ISO-8601 문자열로 저장한다. OpenSearch는 이 형식을 date 타입으로 자동 인식한다.
  private String createdAt;

  public static ContentDocument from(Content content) {
    return ContentDocument.builder()
        .id(content.getId())
        .title(content.getTitle())
        .description(content.getDescription())
        .tags(content.getTags())
        .type(content.getType())
        .averageRating(content.getAverageRating())
        .reviewCount(content.getReviewCount())
        .createdAt(content.getCreatedAt().toString())
        .build();
  }
}