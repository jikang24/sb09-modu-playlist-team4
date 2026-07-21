package com.mopl.domain.content.infrastructure;

import com.mopl.domain.content.domain.ContentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 콘텐츠 JPA 엔티티
 *
 * DB와의 매핑만 담당, 비즈니스 로직 없음
 * 순수 도메인 Content와 ContentMapper를 통해 변환
 */
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
public class ContentJpaEntity {

  @Id
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
  private List<ContentTagJpaEntity> tags = new ArrayList<>();


  public static ContentJpaEntity of(UUID id, ContentType type, String externalId,
      String title, String description, String thumbnailUrl,
      BigDecimal averageRating, int reviewCount) {
    ContentJpaEntity entity = new ContentJpaEntity();
    entity.id = id;
    entity.type = type;
    entity.externalId = externalId;
    entity.title = title;
    entity.description = description;
    entity.thumbnailUrl = thumbnailUrl;
    entity.averageRating = averageRating;
    entity.reviewCount = reviewCount;
    return entity;
  }

  public void replaceTags(List<ContentTagJpaEntity> newTags) {
    this.tags.clear();
    this.tags.addAll(newTags);
  }

  /** 이미 영속화된 엔티티의 필드만 갱신 (컬렉션은 syncTags로 별도 처리) */
  public void update(ContentType type, String title, String description, String thumbnailUrl,
      BigDecimal averageRating, int reviewCount) {
    this.type = type;
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
    this.averageRating = averageRating;
    this.reviewCount = reviewCount;
  }

  /**
   * 태그 값 기준으로 기존 컬렉션을 제자리에서 맞춰준다 (전체 삭제 후 재삽입 X)
   * → 값이 그대로인 태그는 건드리지 않아 content_tags(content_id, tag) 유니크 제약과 충돌하지 않음
   * (참고: replaceTags처럼 매번 새 엔티티로 통째로 갈아끼우면, 겹치는 태그가 있을 때
   *  삭제보다 삽입이 먼저 실행돼 유니크 제약 위반이 났었음)
   */
  public void syncTags(List<String> tagValues) {
    Set<String> desired = new HashSet<>(tagValues);
    this.tags.removeIf(t -> !desired.contains(t.getTag()));

    Set<String> current = new HashSet<>();
    for (ContentTagJpaEntity t : this.tags) {
      current.add(t.getTag());
    }
    for (String tag : tagValues) {
      if (!current.contains(tag)) {
        this.tags.add(ContentTagJpaEntity.of(this, tag));
      }
    }
  }
}