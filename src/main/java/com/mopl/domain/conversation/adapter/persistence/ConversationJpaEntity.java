package com.mopl.domain.conversation.application.port.out.persistence;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "conversations")
@Getter
@RequiredArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ConversationJpaEntity {

  @Id
  private UUID id;

  @ElementCollection
  @CollectionTable(name = "conversation_participants",
  joinColumns = @JoinColumn(name="conversation_id"))
  @Column(name="user_id")
  private final List<UUID> participants;
  private LocalDateTime createdAt;



}
