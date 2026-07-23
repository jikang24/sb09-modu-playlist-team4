package com.mopl.domain.dm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.config.QueryDslConfig;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.response.CursorPageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  @PersistenceContext
  private EntityManager entityManager;

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
  @DisplayName("여러 대화방의 최근 메시지를 한 번에 조회한다")
  void findLatestByConversationIds_success() {

    UUID conversation1 = UUID.randomUUID();
    UUID conversation2 = UUID.randomUUID();
    UUID conversation3WithNoMessage = UUID.randomUUID();

    adapter.save(new DirectMessage(
        UUID.randomUUID(), conversation1, UUID.randomUUID(), UUID.randomUUID(),
        "conv1-old", Instant.parse("2026-01-01T00:00:00Z"), false
    ));
    DirectMessage conv1Latest = adapter.save(new DirectMessage(
        UUID.randomUUID(), conversation1, UUID.randomUUID(), UUID.randomUUID(),
        "conv1-new", Instant.parse("2026-01-02T00:00:00Z"), false
    ));
    DirectMessage conv2Latest = adapter.save(new DirectMessage(
        UUID.randomUUID(), conversation2, UUID.randomUUID(), UUID.randomUUID(),
        "conv2-only", Instant.parse("2026-01-01T12:00:00Z"), false
    ));

    Map<UUID, DirectMessage> result = adapter.findLatestByConversationIds(
        List.of(conversation1, conversation2, conversation3WithNoMessage)
    );

    assertThat(result).hasSize(2);
    assertThat(result.get(conversation1).getId()).isEqualTo(conv1Latest.getId());
    assertThat(result.get(conversation1).getContent()).isEqualTo("conv1-new");
    assertThat(result.get(conversation2).getId()).isEqualTo(conv2Latest.getId());
    assertThat(result).doesNotContainKey(conversation3WithNoMessage);
  }

  @Test
  @DisplayName("대화방 ID 목록이 비어있으면 빈 Map을 반환한다")
  void findLatestByConversationIds_empty() {

    Map<UUID, DirectMessage> result = adapter.findLatestByConversationIds(List.of());

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("여러 대화방 중 안 읽은 메시지가 있는 대화방만 한 번에 조회한다")
  void findConversationIdsWithUnread_success() {

    UUID myId = UUID.randomUUID();
    UUID conversationWithUnread = UUID.randomUUID();
    UUID conversationAllRead = UUID.randomUUID();
    UUID conversationNoMessage = UUID.randomUUID();

    adapter.save(new DirectMessage(
        UUID.randomUUID(), conversationWithUnread, UUID.randomUUID(), myId,
        "안 읽음", Instant.now(), false
    ));
    adapter.save(new DirectMessage(
        UUID.randomUUID(), conversationAllRead, UUID.randomUUID(), myId,
        "읽음", Instant.now(), true
    ));

    Set<UUID> result = adapter.findConversationIdsWithUnread(
        List.of(conversationWithUnread, conversationAllRead, conversationNoMessage), myId
    );

    assertThat(result).containsExactly(conversationWithUnread);
  }

  @Test
  @DisplayName("대화방 ID 목록이 비어있으면 빈 Set을 반환한다")
  void findConversationIdsWithUnread_empty() {

    Set<UUID> result = adapter.findConversationIdsWithUnread(List.of(), UUID.randomUUID());

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("주어진 시각까지 내가 수신자인 안 읽은 메시지를 전부 읽음 처리한다")
  void markAllAsReadUpTo_success() {

    UUID conversationId = UUID.randomUUID();
    UUID receiver = UUID.randomUUID();

    DirectMessage older = adapter.save(new DirectMessage(
        UUID.randomUUID(), conversationId, UUID.randomUUID(), receiver,
        "old-unread", Instant.parse("2026-01-01T00:00:00Z"), false
    ));
    DirectMessage latest = adapter.save(new DirectMessage(
        UUID.randomUUID(), conversationId, UUID.randomUUID(), receiver,
        "latest-unread", Instant.parse("2026-01-02T00:00:00Z"), false
    ));
    // 이후에 온 메시지는 이번 읽음 처리 대상이 아니어야 한다
    DirectMessage future = adapter.save(new DirectMessage(
        UUID.randomUUID(), conversationId, UUID.randomUUID(), receiver,
        "future-unread", Instant.parse("2026-01-03T00:00:00Z"), false
    ));
    // 다른 사람이 수신자인 메시지는 건드리면 안 된다
    DirectMessage otherReceiver = adapter.save(new DirectMessage(
        UUID.randomUUID(), conversationId, UUID.randomUUID(), UUID.randomUUID(),
        "not-mine", Instant.parse("2026-01-01T12:00:00Z"), false
    ));

    adapter.markAllAsReadUpTo(conversationId, receiver, latest.getCreatedAt());
    // 벌크 UPDATE는 영속성 컨텍스트를 거치지 않아, 이미 로드돼 있던 엔티티들이
    // 갱신된 값을 반영하도록 영속성 컨텍스트를 비우고 DB에서 다시 읽는다.
    entityManager.clear();

    assertThat(adapter.findById(older.getId()).orElseThrow().isRead()).isTrue();
    assertThat(adapter.findById(latest.getId()).orElseThrow().isRead()).isTrue();
    assertThat(adapter.findById(future.getId()).orElseThrow().isRead()).isFalse();
    assertThat(adapter.findById(otherReceiver.getId()).orElseThrow().isRead()).isFalse();
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

  @Test
  @DisplayName("동시각(같은 createdAt) 메시지도 id 타이브레이크로 누락/중복 없이 페이징된다")
  void findList_sameCreatedAt_tieBreaksById() {

    UUID conversationId = UUID.randomUUID();
    Instant sameInstant = Instant.parse("2026-01-01T00:00:00Z");
    UUID smallerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID largerId = UUID.fromString("00000000-0000-0000-0000-000000000002");

    adapter.save(new DirectMessage(
        smallerId, conversationId, UUID.randomUUID(), UUID.randomUUID(),
        "same-instant-1", sameInstant, false
    ));
    adapter.save(new DirectMessage(
        largerId, conversationId, UUID.randomUUID(), UUID.randomUUID(),
        "same-instant-2", sameInstant, false
    ));

    // 첫 번째 메시지(smallerId)까지 읽은 상태로 다음 페이지를 요청하면,
    // 같은 시각의 두 번째 메시지(largerId)가 누락되지 않고 정확히 조회돼야 한다.
    DirectMessageSearchCondition condition = new DirectMessageSearchCondition(
        sameInstant.toString(),
        smallerId,
        10,
        SortDirection.ASCENDING,
        "createdAt"
    );

    CursorPageResponse<DirectMessage> result = adapter.findList(conversationId, condition);

    assertThat(result.data()).hasSize(1);
    assertThat(result.data().get(0).getId()).isEqualTo(largerId);
  }
}