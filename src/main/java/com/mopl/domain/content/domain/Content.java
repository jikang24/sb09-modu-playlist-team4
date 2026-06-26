package com.mopl.domain.content.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "contents",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"type", "external_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Content {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 10)
  private ContentType type;

  @Column(name = "external_id", nullable = false, length = 100)
  private String externalId;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "thumbnail_url", length = 500)
  private String thumbnailUrl;

  @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
  private BigDecimal averageRating = BigDecimal.ZERO;


  @Column(name = "review_count", nullable = false)
  private int reviewCount = 0;

  @CreatedDate
  @Column(columnDefinition = "timestamp with time zone", updatable = false, nullable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(columnDefinition = "timestamp with time zone", nullable = false)
  private Instant updatedAt;


  @OneToMany(mappedBy = "content", cascade = CascadeType.ALL,
      orphanRemoval = true, fetch = FetchType.LAZY)
  private List<ContentTag> tags = new ArrayList<>();

  @Builder
  private Content(ContentType type, String externalId, String title,
      String description, String thumbnailUrl) {
    this.type = type;
    this.externalId = externalId;
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
  }

  public static Content create(ContentType type, String externalId,
      String title, String description, String thumbnailUrl) {
    return Content.builder()
        .type(type)
        .externalId(externalId)
        .title(title)
        .description(description)
        .thumbnailUrl(thumbnailUrl)
        .build();
  }


  public void update(String title, String description, String thumbnailUrl) {
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
  }

  public void updateRatingStats(BigDecimal newAverageRating, int newReviewCount) {
    this.averageRating = newAverageRating;
    this.reviewCount = newReviewCount;
  }

  public void addTag(ContentTag tag) {
    this.tags.add(tag);
  }

  public void replaceTags(List<ContentTag> newTags) {
    this.tags.clear();
    this.tags.addAll(newTags);
  }
}