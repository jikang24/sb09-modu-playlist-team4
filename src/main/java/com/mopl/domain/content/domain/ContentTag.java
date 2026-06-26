package com.mopl.domain.content.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "content_tags",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"content_id", "tag"}) // 같은 콘텐츠에 중복 태그 방지
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ContentTag {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  @Column(name = "tag", nullable = false, length = 50)
  private String tag;

  @CreatedDate
  @Column(columnDefinition = "timestamp with time zone", updatable = false, nullable = false)
  private Instant createdAt;

  public static ContentTag create(Content content, String tag) {
    ContentTag contentTag = new ContentTag();
    contentTag.content = content;
    contentTag.tag = tag;
    return contentTag;
  }
}