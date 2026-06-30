package com.mopl.domain.conversation.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
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

  @ElementCollection
  @CollectionTable(name = "conversation_participants",
  joinColumns = @JoinColumn(name="conversation_id"))
  @Column(name="user_id")
  private  List<UUID> participants;
  private  LocalDateTime createdAt;


  @Builder
  private ConversationJpaEntity(UUID id, List<UUID> participants, LocalDateTime createdAt) {
    this.id = id;
    this.participants = participants;
    this.createdAt = createdAt;
  }



}
