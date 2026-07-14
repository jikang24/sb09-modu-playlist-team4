package com.mopl.global.config.cachemixin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Conversation은 생성자가 public이지만, 컴파일 시 -parameters 플래그 없이는 Jackson이
 * 파라미터 이름을 알 수 없어 그대로는 역직렬화가 불가 - Redis 캐시 역직렬화 전용 믹스인 작성.
 *
 * getParticipants()처럼 필드가 아닌 파생 getter는 setter가 없는 "setterless" 프로퍼티라
 * ignoreUnknown만으로는 안 걸러지고 별도 예외로 역직렬화가 실패 - 이름으로 명시적으로 무시.
 */
@JsonIgnoreProperties(value = {"participants"}, ignoreUnknown = true)
public abstract class ConversationCacheMixin {

  @JsonCreator
  public ConversationCacheMixin(
      @JsonProperty("id") UUID id,
      @JsonProperty("participant1Id") UUID participant1Id,
      @JsonProperty("participant2Id") UUID participant2Id,
      @JsonProperty("createdAt") Instant createdAt) {
  }
}