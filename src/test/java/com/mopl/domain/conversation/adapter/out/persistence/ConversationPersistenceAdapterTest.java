package com.mopl.domain.conversation.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mopl.domain.conversation.application.dto.ConversationSearchCondition;
import com.mopl.domain.conversation.application.port.out.LoadUserPort;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.domain.dm.application.port.in.GetConversationIdsByContentUseCase;
import com.mopl.global.config.QueryDslConfig;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.response.CursorPageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import({
    ConversationPersistenceAdapter.class,
    ConversationPersistenceMapperImpl.class,
    QueryDslConfig.class
})
class ConversationPersistenceAdapterTest {

  @Autowired
  private ConversationPersistenceAdapter adapter;

  @Autowired
  private ConversationRepository repository;

  @MockitoBean
  private GetConversationIdsByContentUseCase getConversationIdsByContentUseCase;

  @MockitoBean
  private LoadUserPort loadUserPort;

  @Test
  @DisplayName("대화 저장 성공")
  void save_success() {

    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();

    Conversation conversation = Conversation.create(user1, user2);

    Conversation saved = adapter.save(conversation);

    assertThat(saved.getId()).isEqualTo(conversation.getId());
    assertThat(repository.findById(saved.getId())).isPresent();
  }

  @Test
  @DisplayName("ID로 조회 성공")
  void findById_success() {

    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();

    Conversation saved = adapter.save(
        Conversation.create(user1, user2));

    Optional<Conversation> result =
        adapter.findById(saved.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(saved.getId());
    assertThat(result.get().getParticipant1Id()).isEqualTo(user1);
    assertThat(result.get().getParticipant2Id()).isEqualTo(user2);
  }

  @Test
  @DisplayName("참여자로 조회 성공")
  void findByParticipants_success() {

    UUID me = UUID.randomUUID();
    UUID other = UUID.randomUUID();

    Conversation saved = adapter.save(
        Conversation.create(me, other));

    Optional<Conversation> result =
        adapter.findByParticipants(me, other);

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(saved.getId());
  }

  @Test
  @DisplayName("참여자가 없으면 Optional.empty 반환")
  void findByParticipants_fail() {

    Optional<Conversation> result =
        adapter.findByParticipants(
            UUID.randomUUID(),
            UUID.randomUUID());

    assertThat(result).isEmpty();
  }
  @Test
  @DisplayName("첫 페이지 조회")
  void findList_firstPage() {

    UUID me = UUID.randomUUID();
    UUID other = UUID.randomUUID();

    adapter.save(new Conversation(
        UUID.randomUUID(),
        me,
        other,
        Instant.parse("2026-01-01T00:00:00Z")
    ));

    ConversationSearchCondition condition =
        new ConversationSearchCondition(
            null,
            null,
            null,
            10,
            "createdAt",
            SortDirection.DESCENDING
        );

    CursorPageResponse<Conversation> result =
        adapter.findList(me, condition);

    assertThat(result.data()).hasSize(1);
    assertThat(result.hasNext()).isFalse();
  }
  @Test
  @DisplayName("키워드 검색 결과 없음")
  void findList_keyword_empty() {

    UUID me = UUID.randomUUID();

    when(getConversationIdsByContentUseCase.findConversationIdsByContent(anyString()))
        .thenReturn(List.of());

    when(loadUserPort.findUserIdsByNameLike(anyString()))
        .thenReturn(List.of());

    ConversationSearchCondition condition =
        new ConversationSearchCondition(
            "hello",
            null,
            null,
            10,
            "createdAt",
            SortDirection.DESCENDING
        );

    CursorPageResponse<Conversation> result =
        adapter.findList(me, condition);

    assertThat(result.data()).isEmpty();
    assertThat(result.hasNext()).isFalse();
  }
  @Test
  @DisplayName("대화 내용으로 조회")
  void findList_keyword_byConversationId() {

    UUID me = UUID.randomUUID();
    UUID other = UUID.randomUUID();

    Conversation saved = adapter.save(
        Conversation.create(me, other));

    when(getConversationIdsByContentUseCase.findConversationIdsByContent(anyString()))
        .thenReturn(List.of(saved.getId()));

    when(loadUserPort.findUserIdsByNameLike(anyString()))
        .thenReturn(List.of());

    ConversationSearchCondition condition =
        new ConversationSearchCondition(
            "hello",
            null,
            null,
            10,
            "createdAt",
            SortDirection.DESCENDING
        );

    CursorPageResponse<Conversation> result =
        adapter.findList(me, condition);

    assertThat(result.data()).hasSize(1);
  }
  @Test
  @DisplayName("사용자 이름 검색")
  void findList_keyword_byUser() {

    UUID me = UUID.randomUUID();
    UUID other = UUID.randomUUID();

    adapter.save(Conversation.create(me, other));

    when(getConversationIdsByContentUseCase.findConversationIdsByContent(anyString()))
        .thenReturn(List.of());

    when(loadUserPort.findUserIdsByNameLike(anyString()))
        .thenReturn(List.of(other));

    ConversationSearchCondition condition =
        new ConversationSearchCondition(
            "kim",
            null,
            null,
            10,
            "createdAt",
            SortDirection.DESCENDING
        );

    CursorPageResponse<Conversation> result =
        adapter.findList(me, condition);

    assertThat(result.data()).hasSize(1);
  }
  @Test
  @DisplayName("ASC 커서 조회")
  void findList_cursor_asc() {

    UUID me = UUID.randomUUID();
    UUID other = UUID.randomUUID();

    adapter.save(new Conversation(
        UUID.randomUUID(),
        me,
        other,
        Instant.parse("2026-01-01T00:00:00Z")
    ));

    ConversationSearchCondition condition =
        new ConversationSearchCondition(
            null,
            "2025-12-31T00:00:00Z",
            UUID.randomUUID(),
            10,
            "createdAt",
            SortDirection.ASCENDING
        );

    CursorPageResponse<Conversation> result =
        adapter.findList(me, condition);

    assertThat(result.data()).hasSize(1);
  }
  @Test
  @DisplayName("DESC 커서 조회")
  void findList_cursor_desc() {

    UUID me = UUID.randomUUID();
    UUID other = UUID.randomUUID();

    adapter.save(new Conversation(
        UUID.randomUUID(),
        me,
        other,
        Instant.parse("2026-01-01T00:00:00Z")
    ));

    ConversationSearchCondition condition =
        new ConversationSearchCondition(
            null,
            "2026-12-31T00:00:00Z",
            UUID.randomUUID(),
            10,
            "createdAt",
            SortDirection.DESCENDING
        );

    CursorPageResponse<Conversation> result =
        adapter.findList(me, condition);

    assertThat(result.data()).hasSize(1);
  }
  @Test
  @DisplayName("다음 페이지 존재")
  void findList_hasNext() {

    UUID me = UUID.randomUUID();

    for (int i = 0; i < 11; i++) {
      adapter.save(new Conversation(
          UUID.randomUUID(),
          me,
          UUID.randomUUID(),
          Instant.now().plusSeconds(i)
      ));
    }

    ConversationSearchCondition condition =
        new ConversationSearchCondition(
            null,
            null,
            null,
            10,
            "createdAt",
            SortDirection.DESCENDING
        );

    CursorPageResponse<Conversation> result =
        adapter.findList(me, condition);

    assertThat(result.data()).hasSize(10);
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  @DisplayName("동시각(같은 createdAt) 대화도 id 타이브레이크로 누락/중복 없이 페이징된다")
  void findList_sameCreatedAt_tieBreaksById() {

    UUID me = UUID.randomUUID();
    Instant sameInstant = Instant.parse("2026-01-01T00:00:00Z");
    UUID smallerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID largerId = UUID.fromString("00000000-0000-0000-0000-000000000002");

    adapter.save(new Conversation(smallerId, me, UUID.randomUUID(), sameInstant));
    adapter.save(new Conversation(largerId, me, UUID.randomUUID(), sameInstant));

    // 첫 번째 대화(smallerId)까지 읽은 상태로 다음 페이지를 요청하면,
    // 같은 시각의 두 번째 대화(largerId)가 누락되지 않고 정확히 조회돼야 한다.
    ConversationSearchCondition condition = new ConversationSearchCondition(
        null,
        sameInstant.toString(),
        smallerId,
        10,
        "createdAt",
        SortDirection.ASCENDING
    );

    CursorPageResponse<Conversation> result = adapter.findList(me, condition);

    assertThat(result.data()).hasSize(1);
    assertThat(result.data().get(0).getId()).isEqualTo(largerId);
  }
}
