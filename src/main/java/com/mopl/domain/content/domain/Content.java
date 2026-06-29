package com.mopl.domain.content.domain;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Content {

  private final UUID id;
  private final ContentType type;
  private final String externalId;
  private String title;
  private String description;
  private String thumbnailUrl;
  private BigDecimal averageRating;
  private int reviewCount;
  private final Instant createdAt;
  private Instant updatedAt;
  private List<String> tags;

  // ──────────────────────────────────────────────
  // 생성자 (외부에서 직접 호출 X → 팩토리 메서드 사용)
  // ──────────────────────────────────────────────

  private Content(UUID id, ContentType type, String externalId,
      String title, String description, String thumbnailUrl,
      BigDecimal averageRating, int reviewCount,
      Instant createdAt, Instant updatedAt, List<String> tags) {
    this.id = id;
    this.type = type;
    this.externalId = externalId;
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
    this.averageRating = averageRating;
    this.reviewCount = reviewCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.tags = tags != null ? tags : new ArrayList<>();
  }

  /**
   * 신규 콘텐츠 생성 팩토리 메서드
   * id, createdAt, updatedAt은 도메인이 직접 생성
   */

  public static Content create(ContentType type, String externalId,
      String title, String description, String thumbnailUrl,
      List<String> tags) {

    if (type == null)
      throw new MoplException(ErrorCode.INVALID_INPUT);
    if (externalId == null || externalId.isBlank())
      throw new MoplException(ErrorCode.INVALID_INPUT);
    if (title == null || title.isBlank())
      throw new MoplException(ErrorCode.INVALID_INPUT);

    Instant now = Instant.now();
    return new Content(
        UUID.randomUUID(), type, externalId,
        title, description, thumbnailUrl,
        BigDecimal.ZERO, 0,
        now, now, tags
    );
  }

  /**
   * DB에서 복원할 때 사용하는 팩토리 메서드
   */
  public static Content restore(UUID id, ContentType type, String externalId,
      String title, String description, String thumbnailUrl,
      BigDecimal averageRating, int reviewCount,
      Instant createdAt, Instant updatedAt, List<String> tags) {
    return new Content(id, type, externalId, title, description, thumbnailUrl,
        averageRating, reviewCount, createdAt, updatedAt, tags);
  }

  /** 콘텐츠 정보 수정 */
  public void update(String title, String description, String thumbnailUrl, List<String> tags) {
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
    this.tags = tags != null ? tags : new ArrayList<>();
    this.updatedAt = Instant.now();
  }

  /** 평점/리뷰 수 갱신 (Review 모듈 이벤트 수신 시) */
  public void updateRatingStats(BigDecimal newAverageRating, int newReviewCount) {
    this.averageRating = newAverageRating;
    this.reviewCount = newReviewCount;
    this.updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public ContentType getType() { return type; }
  public String getExternalId() { return externalId; }
  public String getTitle() { return title; }
  public String getDescription() { return description; }
  public String getThumbnailUrl() { return thumbnailUrl; }
  public BigDecimal getAverageRating() { return averageRating; }
  public int getReviewCount() { return reviewCount; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public List<String> getTags() { return List.copyOf(tags); }
}