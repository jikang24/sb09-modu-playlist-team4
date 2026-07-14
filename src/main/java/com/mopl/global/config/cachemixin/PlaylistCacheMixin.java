package com.mopl.global.config.cachemixin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mopl.domain.playlist.domain.Playlist;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Playlist는 생성자가 private이고 순수 도메인이라 Jackson 관련 애노테이션을 붙일 수 없다(붙여서도 안 됨).
 * Redis 캐시 역직렬화 전용으로, Playlist.restore(...)를 생성자처럼 쓰도록 알려주는 믹스인.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class PlaylistCacheMixin {

  @JsonCreator
  public static Playlist restore(
      @JsonProperty("id") UUID id,
      @JsonProperty("ownerId") UUID ownerId,
      @JsonProperty("title") String title,
      @JsonProperty("description") String description,
      @JsonProperty("createdAt") Instant createdAt,
      @JsonProperty("updatedAt") Instant updatedAt,
      @JsonProperty("contentIds") List<UUID> contentIds) {
    return null; // 본문은 호출되지 않음 - Jackson이 시그니처만 보고 Playlist.restore를 직접 호출
  }
}