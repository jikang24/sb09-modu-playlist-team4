package com.mopl.domain.dm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.config.QueryDslConfig;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.response.CursorPageResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import({
    DirectMessagePersistenceAdapter.class,
    DirectMessagePersistenceMapperImpl.class,
    QueryDslConfig.class
})
class DirectMessagePersistenceAdapterTest {

  @Autowired
  private DirectMessagePersistenceAdapter adapter;

  @Autowired
  private DirectMessageRepository repository;

  @Test
  @DisplayName("메시지 저장 및 조회 성공")
  void saveAndFind_success() {

    UUID conversationId = UUID.randomUUID();
    UUID sender = UUID.randomUUID();
    UUID receiver = UUID.randomUUID();

    DirectMessage dm =
        DirectMessage.create(conversationId, sender, receiver, "hello");

    DirectMessage saved = adapter.save(dm);

    Optional<DirectMessage> found = adapter.findById(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getContent()).isEqualTo("hello");
    assertThat(found.get().getConversationId()).isEqualTo(conversationId);
  }

  @Test
  @DisplayName("ID로 조회 성공")
  void findById_success() {

    UUID conversationId = UUID.randomUUID();

    DirectMessage saved = adapter.save(
        DirectMessage.create(
            conversationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "hello"));

    Optional<DirectMessage> result =
        adapter.findById(saved.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(saved.getId());
  }

  @Test
  @DisplayName("가장 최근 메시지 조회")
  void findLatestByConversationId_success() {

    UUID conversationId = UUID.randomUUID();

    adapter.save(new DirectMessage(
        UUID.randomUUID(),
        conversationId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "old",
        Instant.parse("2026-01-01T00:00:00Z"),
        false
    ));

    DirectMessage latest = adapter.save(new DirectMessage(
        UUID.randomUUID(),
        conversationId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "new",
        Instant.parse("2026-01-02T00:00:00Z"),
        false
    ));

    Optional<DirectMessage> result =
        adapter.findLatestByConversationId(conversationId);

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(latest.getId());
    assertThat(result.get().getContent()).isEqualTo("new");
  }

  @Test
  @DisplayName("안 읽은 메시지가 존재한다")
  void hasUnread_true() {

    UUID conversationId = UUID.randomUUID();
    UUID receiver = UUID.randomUUID();

    adapter.save(new DirectMessage(
        UUID.randomUUID(),
        conversationId,
        UUID.randomUUID(),
        receiver,
        "hello",
        Instant.now(),
        false
    ));

    assertThat(
        adapter.hasUnRead(conversationId, receiver)
    ).isTrue();
  }

  @Test
  @DisplayName("안 읽은 메시지가 존재하지 않는다")
  void hasUnread_false() {

    UUID conversationId = UUID.randomUUID();
    UUID receiver = UUID.randomUUID();

    adapter.save(new DirectMessage(
        UUID.randomUUID(),
        conversationId,
        UUID.randomUUID(),
        receiver,
        "hello",
        Instant.now(),
        true
    ));

    assertThat(
        adapter.hasUnRead(conversationId, receiver)
    ).isFalse();
  }
  @Test
  @DisplayName("내용으로 conversationId 조회")
  void findConversationIdsByContent_success() {

    UUID conversation1 = UUID.randomUUID();
    UUID conversation2 = UUID.randomUUID();

    adapter.save(new DirectMessage(
        UUID.randomUUID(),
        conversation1,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "hello world",
        Instant.now(),
        false
    ));

    adapter.save(new DirectMessage(
        UUID.randomUUID(),
        conversation2,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "hello java",
        Instant.now(),
        false
    ));

    adapter.save(new DirectMessage(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "spring boot",
        Instant.now(),
        false
    ));

    assertThat(adapter.findConversationIdsByContent("hello"))
        .containsExactlyInAnyOrder(conversation1, conversation2);
  }

  @Test
  @DisplayName("첫 페이지 조회")
  void findList_firstPage() {

    UUID conversationId = UUID.randomUUID();

    adapter.save(new DirectMessage(
        UUID.randomUUID(),
        conversationId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "message",
        Instant.parse("2026-01-01T00:00:00Z"),
        false
    ));

    DirectMessageSearchCondition condition =
        new DirectMessageSearchCondition(
            null,
            null,
            10,
            SortDirection.DESCENDING,
            "createdAt"
        );

    CursorPageResponse<DirectMessage> result =
        adapter.findList(conversationId, condition);

    assertThat(result.data()).hasSize(1);
    assertThat(result.hasNext()).isFalse();
  }

  @Test
  @DisplayName("ASC 커서 조회")
  void findList_cursorAscending() {

    UUID conversationId = UUID.randomUUID();

    adapter.save(new DirectMessage(
        UUID.randomUUID(),
        conversationId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "message",
        Instant.parse("2026-01-02T00:00:00Z"),
        false
    ));

    DirectMessageSearchCondition condition =
        new DirectMessageSearchCondition(
            "2026-01-01T00:00:00Z",
            UUID.randomUUID(),
            10,
            SortDirection.ASCENDING,
            "createdAt"
        );

    CursorPageResponse<DirectMessage> result =
        adapter.findList(conversationId, condition);

    assertThat(result.data()).hasSize(1);
  }

  @Test
  @DisplayName("DESC 커서 조회")
  void findList_cursorDescending() {

    UUID conversationId = UUID.randomUUID();

    adapter.save(new DirectMessage(
        UUID.randomUUID(),
        conversationId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "message",
        Instant.parse("2026-01-01T00:00:00Z"),
        false
    ));

    DirectMessageSearchCondition condition =
        new DirectMessageSearchCondition(
            "2026-01-02T00:00:00Z",
            UUID.randomUUID(),
            10,
            SortDirection.DESCENDING,
            "createdAt"
        );

    CursorPageResponse<DirectMessage> result =
        adapter.findList(conversationId, condition);

    assertThat(result.data()).hasSize(1);
  }

  @Test
  @DisplayName("다음 페이지가 존재한다")
  void findList_hasNext() {

    UUID conversationId = UUID.randomUUID();

    for (int i = 0; i < 11; i++) {
      adapter.save(new DirectMessage(
          UUID.randomUUID(),
          conversationId,
          UUID.randomUUID(),
          UUID.randomUUID(),
          "message" + i,
          Instant.now().plusSeconds(i),
          false
      ));
    }

    DirectMessageSearchCondition condition =
        new DirectMessageSearchCondition(
            null,
            null,
            10,
            SortDirection.DESCENDING,
            "createdAt"
        );

    CursorPageResponse<DirectMessage> result =
        adapter.findList(conversationId, condition);

    assertThat(result.data()).hasSize(10);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isNotNull();
    assertThat(result.nextIdAfter()).isNotNull();
  }
}