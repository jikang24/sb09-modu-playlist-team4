package com.mopl.domain.conversation.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationJpaEntityTest {

  @Test
  void builder로_ConversationJpaEntity를_생성한다() {
    UUID id = UUID.randomUUID();
    UUID participant1Id = UUID.randomUUID();
    UUID participant2Id = UUID.randomUUID();
    Instant createdAt = Instant.now();

    ConversationJpaEntity entity = ConversationJpaEntity.builder()
        .id(id)
        .participant1Id(participant1Id)
        .participant2Id(participant2Id)
        .createdAt(createdAt)
        .build();

    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getParticipant1Id()).isEqualTo(participant1Id);
    assertThat(entity.getParticipant2Id()).isEqualTo(participant2Id);
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
  }
}