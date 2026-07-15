package com.mopl.domain.dm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DirectMessageJpaEntityTest {

  @Test
  @DisplayName("Builder로 DirectMessageJpaEntity를 생성한다")
  void builder_success() {

    UUID id = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    Instant createdAt = Instant.now();

    DirectMessageJpaEntity entity = DirectMessageJpaEntity.builder()
        .id(id)
        .conversationId(conversationId)
        .senderId(senderId)
        .receiverId(receiverId)
        .content("안녕하세요")
        .createdAt(createdAt)
        .read(false)
        .build();

    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getConversationId()).isEqualTo(conversationId);
    assertThat(entity.getSenderId()).isEqualTo(senderId);
    assertThat(entity.getReceiverId()).isEqualTo(receiverId);
    assertThat(entity.getContent()).isEqualTo("안녕하세요");
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(entity.isRead()).isFalse();
  }
}