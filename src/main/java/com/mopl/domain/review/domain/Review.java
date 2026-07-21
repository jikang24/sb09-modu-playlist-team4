package com.mopl.domain.review.domain;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Review {

  @Id
  private UUID id;

  @Column(name = "content_id", nullable = false)
  private UUID contentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private BigDecimal rating;

  @Column(columnDefinition = "TEXT")
  private String text;

  @Column(name = "author_name", nullable = false)
  private String authorName;

  @Column(name = "author_profile_image_url")
  private String authorProfileImageUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  private Review(UUID id, UUID contentId, UUID userId, BigDecimal rating, String text,
      String authorName, String authorProfileImageUrl, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.contentId = contentId;
    this.userId = userId;
    this.rating = rating;
    this.text = text;
    this.authorName = authorName;
    this.authorProfileImageUrl = authorProfileImageUrl;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Review create(UUID contentId, UUID userId, BigDecimal rating, String text,
      String authorName, String authorProfileImageUrl) {
    if (contentId == null || userId == null) {
      throw new MoplException(ErrorCode.INVALID_INPUT);
    }
    validateRating(rating);

    Instant now = Instant.now();
    return new Review(UUID.randomUUID(), contentId, userId, rating, text,
        authorName, authorProfileImageUrl, now, now);
  }

  public void update(BigDecimal rating, String text) {
    validateRating(rating);
    this.rating = rating;
    this.text = text;
    this.updatedAt = Instant.now();
  }

  public void updateAuthorSnapshot(String authorName, String authorProfileImageUrl) {
    this.authorName = authorName;
    this.authorProfileImageUrl = authorProfileImageUrl;
    // updatedAt은 안 건드림 - 리뷰 "내용"이 바뀐 게 아니라 표시 정보만 갱신된 거라서
  }

  // DB CHECK 제약(rating >= 0.5 AND rating <= 5.0)과 동일한 범위
  // DB 저장 시점에 CHECK violation으로 500으로 남는것을 막기 위함
  private static void validateRating(BigDecimal rating) {
    if (rating == null
        || rating.compareTo(BigDecimal.valueOf(0.5)) < 0
        || rating.compareTo(BigDecimal.valueOf(5)) > 0) {
      throw new MoplException(ErrorCode.INVALID_INPUT);
    }
  }
}