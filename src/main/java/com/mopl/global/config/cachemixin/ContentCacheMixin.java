package com.mopl.global.config.cachemixin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Content는 생성자가 private이고 순수 도메인이라 Jackson 관련 어노테이션을 붙이면 안됨.
 * Redis 캐시 역직렬화 전용으로, Content.restore(...)를 생성자처럼 쓰도록 알려주는 믹스인.
 * Jackson은 이 클래스의 애노테이션만 참고하고 실제 호출은 Content.restore(...)로 진행.
 *
 * ignoreUnknown=true: isManuallyCreated()처럼 필드가 아닌 파생 getter는 직렬화 시 JSON에 같이
 * 찍히지만 restore(...)의 생성자 파라미터엔 없으므로, 역직렬화 시 무시하도록 허용한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ContentCacheMixin {

  @JsonCreator
  public static Content restore(
      @JsonProperty("id") UUID id,
      @JsonProperty("type") ContentType type,
      @JsonProperty("externalId") String externalId,
      @JsonProperty("title") String title,
      @JsonProperty("description") String description,
      @JsonProperty("thumbnailUrl") String thumbnailUrl,
      @JsonProperty("averageRating") BigDecimal averageRating,
      @JsonProperty("reviewCount") int reviewCount,
      @JsonProperty("createdAt") Instant createdAt,
      @JsonProperty("updatedAt") Instant updatedAt,
      @JsonProperty("tags") List<String> tags) {
    return null; // 본문은 호출되지 않음 - Jackson이 시그니처만 보고 Content.restore를 직접 호출
  }
}