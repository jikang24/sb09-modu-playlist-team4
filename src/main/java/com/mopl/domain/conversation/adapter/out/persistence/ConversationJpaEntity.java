package com.mopl.domain.conversation.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationJpaEntity {

  @Id
  private  UUID id;
  @Column(name = "participant1_id", nullable = false)
  private UUID participant1Id;

  @Column(name = "participant2_id", nullable = false)
  private UUID participant2Id;

  private  LocalDateTime createdAt;


  @Builder
  private ConversationJpaEntity(UUID id, UUID participant1Id, UUID participant2Id, LocalDateTime createdAt) {
    this.id = id;
    this.participant1Id = participant1Id;
    this.participant2Id = participant2Id;
    this.createdAt = createdAt;
  }



}
