package com.mopl.domain.content.infrastructure;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * [Infrastructure] 콘텐츠 태그 JPA 엔티티
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "content_tags",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"content_id", "tag"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ContentTagJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_id", nullable = false)
  private ContentJpaEntity content;

  @Column(name = "tag", nullable = false, length = 50)
  private String tag;

  @CreatedDate
  @Column(columnDefinition = "timestamp with time zone", updatable = false, nullable = false)
  private Instant createdAt;

  public static ContentTagJpaEntity of(ContentJpaEntity content, String tag) {
    ContentTagJpaEntity entity = new ContentTagJpaEntity();
    entity.content = content;
    entity.tag = tag;
    return entity;
  }
}