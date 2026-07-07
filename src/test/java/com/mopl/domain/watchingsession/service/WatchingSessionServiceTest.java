package com.mopl.domain.watchingsession.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.watchingsession.adapter.port.LoadContentPort;
import com.mopl.domain.watchingsession.adapter.port.LoadUserPort;
import com.mopl.domain.watchingsession.domain.WatchingSession;
import com.mopl.domain.watchingsession.dto.WatchingSessionChange;
import com.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.mopl.domain.watchingsession.dto.WatchingSessionSearchRequest;
import com.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.mopl.global.dto.ContentSummary;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class WatchingSessionServiceTest {

  @Mock
  private WatchingSessionRepository watchingSessionRepository;

  @Mock
  private LoadUserPort loadUserPort;

  @Mock
  private LoadContentPort loadContentPort;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @InjectMocks
  private WatchingSessionService watchingSessionService;

  private ContentSummary makeContent(UUID contentId) {
    return new ContentSummary(
        contentId, ContentType.MOVIE, "제목", "설명", "https://thumb.jpg",
        List.of("액션"), BigDecimal.valueOf(4.5), 10);
  }

  private UserSummary makeUser(UUID userId) {
    return new UserSummary(userId, "닉네임", "https://profile.jpg");
  }

  @Nested
  @DisplayName("시청 입장 - enter()")
  class Enter {

    @Test
    @DisplayName("정상 입장 - 유저/콘텐츠 정보를 조회해 DTO로 반환하고, JOIN 브로드캐스트한다")
    void success() {
      UUID watcherId = UUID.randomUUID();
      UUID contentId = UUID.randomUUID();
      WatchingSession session = WatchingSession.create(watcherId, contentId);
      UserSummary user = makeUser(watcherId);
      ContentSummary content = makeContent(contentId);

      given(watchingSessionRepository.enter(watcherId, contentId)).willReturn(session);
      given(loadUserPort.getUserSummary(watcherId)).willReturn(user);
      given(loadContentPort.getContent(contentId)).willReturn(content);
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(3L);

      WatchingSessionDto dto = watchingSessionService.enter(watcherId, contentId);

      assertThat(dto.id()).isEqualTo(session.id());
      assertThat(dto.createdAt()).isEqualTo(session.createdAt());
      assertThat(dto.watcher()).isEqualTo(user);
      assertThat(dto.content()).isEqualTo(content);

      ArgumentCaptor<WatchingSessionChange> captor = ArgumentCaptor.forClass(WatchingSessionChange.class);
      then(messagingTemplate).should().convertAndSend(
          eq("/sub/contents/" + contentId + "/watch"), captor.capture());
      WatchingSessionChange change = captor.getValue();
      assertThat(change.type()).isEqualTo(WatchingSessionChange.ChangeType.JOIN);
      assertThat(change.watchingSession()).isEqualTo(dto);
      assertThat(change.watcherCount()).isEqualTo(3L);
    }
  }

  @Nested
  @DisplayName("시청 퇴장 - leave()")
  class Leave {

    @Test
    @DisplayName("정상 퇴장 - repository.leave 호출 후 /sub/contents/{contentId}/watch 로 LEAVE 브로드캐스트")
    void success() {
      UUID watcherId = UUID.randomUUID();
      UUID contentId = UUID.randomUUID();
      WatchingSession session = WatchingSession.create(watcherId, contentId);
      UserSummary user = makeUser(watcherId);
      ContentSummary content = makeContent(contentId);

      given(watchingSessionRepository.leave(watcherId)).willReturn(session);
      given(loadUserPort.getUserSummary(watcherId)).willReturn(user);
      given(loadContentPort.getContent(contentId)).willReturn(content);
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(1L);

      watchingSessionService.leave(watcherId);

      then(watchingSessionRepository).should().leave(watcherId);

      ArgumentCaptor<WatchingSessionChange> captor = ArgumentCaptor.forClass(WatchingSessionChange.class);
      then(messagingTemplate).should().convertAndSend(
          eq("/sub/contents/" + contentId + "/watch"), captor.capture());
      WatchingSessionChange change = captor.getValue();
      assertThat(change.type()).isEqualTo(WatchingSessionChange.ChangeType.LEAVE);
      assertThat(change.watchingSession().watcher()).isEqualTo(user);
      assertThat(change.watcherCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("보고 있는 세션이 없으면 예외가 그대로 전파된다")
    void fail_notFound() {
      UUID watcherId = UUID.randomUUID();
      doThrow(new MoplException(ErrorCode.WATCHING_SESSION_NOT_FOUND))
          .when(watchingSessionRepository).leave(watcherId);

      assertThatThrownBy(() -> watchingSessionService.leave(watcherId))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.WATCHING_SESSION_NOT_FOUND));
    }
  }

  @Nested
  @DisplayName("특정 사용자의 현재 세션 조회 - getByWatcherId()")
  class GetByWatcherId {

    @Test
    @DisplayName("세션이 있으면 DTO를 반환한다")
    void found() {
      UUID watcherId = UUID.randomUUID();
      UUID contentId = UUID.randomUUID();
      WatchingSession session = WatchingSession.create(watcherId, contentId);

      given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.of(session));
      given(loadUserPort.getUserSummary(watcherId)).willReturn(makeUser(watcherId));
      given(loadContentPort.getContent(contentId)).willReturn(makeContent(contentId));

      WatchingSessionDto dto = watchingSessionService.getByWatcherId(watcherId);

      assertThat(dto).isNotNull();
      assertThat(dto.id()).isEqualTo(session.id());
    }

    @Test
    @DisplayName("세션이 없으면 null을 반환하고 유저/콘텐츠 조회는 하지 않는다")
    void notFound_returnsNull() {
      UUID watcherId = UUID.randomUUID();
      given(watchingSessionRepository.findByWatcherId(watcherId)).willReturn(Optional.empty());

      WatchingSessionDto dto = watchingSessionService.getByWatcherId(watcherId);

      assertThat(dto).isNull();
      then(loadUserPort).shouldHaveNoInteractions();
      then(loadContentPort).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("콘텐츠별 시청 세션 목록 조회 - getByContentId()")
  class GetByContentId {

    private WatchingSessionSearchRequest makeRequest(UUID contentId, int limit) {
      return new WatchingSessionSearchRequest(contentId, null, null, limit, "createdAt", "DESCENDING");
    }

    @Test
    @DisplayName("다음 페이지 없음 - hasNext=false")
    void noNextPage() {
      UUID contentId = UUID.randomUUID();
      UUID watcherId = UUID.randomUUID();
      WatchingSessionSearchRequest request = makeRequest(contentId, 3);
      WatchingSession session = WatchingSession.create(watcherId, contentId);

      given(watchingSessionRepository.findByContentId(request)).willReturn(List.of(session));
      given(loadContentPort.getContent(contentId)).willReturn(makeContent(contentId));
      given(loadUserPort.getUserSummaries(anyCollection()))
          .willReturn(Map.of(watcherId, makeUser(watcherId)));
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(1L);

      CursorPageResponse<WatchingSessionDto> response = watchingSessionService.getByContentId(request);

      assertThat(response.data()).hasSize(1);
      assertThat(response.hasNext()).isFalse();
      assertThat(response.nextCursor()).isNull();
      assertThat(response.nextIdAfter()).isNull();
      assertThat(response.totalCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("다음 페이지 있음 - nextCursor/nextIdAfter가 마지막 세션 기준으로 채워진다")
    void hasNextPage() {
      UUID contentId = UUID.randomUUID();
      UUID watcherId1 = UUID.randomUUID();
      UUID watcherId2 = UUID.randomUUID();
      WatchingSessionSearchRequest request = makeRequest(contentId, 1);
      WatchingSession first = WatchingSession.create(watcherId1, contentId);
      WatchingSession second = WatchingSession.create(watcherId2, contentId);

      given(watchingSessionRepository.findByContentId(request)).willReturn(List.of(first, second));
      given(loadContentPort.getContent(contentId)).willReturn(makeContent(contentId));
      given(loadUserPort.getUserSummaries(anyCollection()))
          .willReturn(Map.of(watcherId1, makeUser(watcherId1), watcherId2, makeUser(watcherId2)));
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(5L);

      CursorPageResponse<WatchingSessionDto> response = watchingSessionService.getByContentId(request);

      assertThat(response.data()).hasSize(1); // limit(1)만큼만 반환
      assertThat(response.hasNext()).isTrue();
      assertThat(response.nextCursor()).isEqualTo(first.createdAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(first.id());
      assertThat(response.totalCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("빈 결과 - data 비어있고 hasNext=false")
    void emptyResult() {
      UUID contentId = UUID.randomUUID();
      WatchingSessionSearchRequest request = makeRequest(contentId, 10);

      given(watchingSessionRepository.findByContentId(request)).willReturn(List.of());
      given(loadContentPort.getContent(contentId)).willReturn(makeContent(contentId));
      given(loadUserPort.getUserSummaries(anyCollection())).willReturn(Map.of());
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(0L);

      CursorPageResponse<WatchingSessionDto> response = watchingSessionService.getByContentId(request);

      assertThat(response.data()).isEmpty();
      assertThat(response.hasNext()).isFalse();
      assertThat(response.totalCount()).isZero();
    }

    @Test
    @DisplayName("N+1 방지 - 콘텐츠는 페이지당 한 번만 조회한다")
    void contentLookedUpOnlyOnce() {
      UUID contentId = UUID.randomUUID();
      WatchingSessionSearchRequest request = makeRequest(contentId, 5);
      List<WatchingSession> sessions = List.of(
          WatchingSession.create(UUID.randomUUID(), contentId),
          WatchingSession.create(UUID.randomUUID(), contentId),
          WatchingSession.create(UUID.randomUUID(), contentId)
      );

      given(watchingSessionRepository.findByContentId(request)).willReturn(sessions);
      given(loadContentPort.getContent(contentId)).willReturn(makeContent(contentId));
      given(loadUserPort.getUserSummaries(anyCollection())).willReturn(Map.of());
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(3L);

      watchingSessionService.getByContentId(request);

      then(loadContentPort).should(times(1)).getContent(contentId);
    }

    @Test
    @DisplayName("N+1 방지 - 유저는 개별 조회 대신 배치로 한 번에 조회하고, watcherId는 중복 제거해서 넘긴다")
    void usersBatchLoadedWithDistinctIds() {
      UUID contentId = UUID.randomUUID();
      UUID watcherId = UUID.randomUUID();
      WatchingSessionSearchRequest request = makeRequest(contentId, 5);
      // 같은 watcherId로 세션 2개 (예: 재입장 이력이 남아있는 케이스 가정)
      List<WatchingSession> sessions = List.of(
          WatchingSession.create(watcherId, contentId),
          WatchingSession.create(watcherId, contentId)
      );

      given(watchingSessionRepository.findByContentId(request)).willReturn(sessions);
      given(loadContentPort.getContent(contentId)).willReturn(makeContent(contentId));
      given(loadUserPort.getUserSummaries(anyCollection()))
          .willReturn(Map.of(watcherId, makeUser(watcherId)));
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(2L);

      watchingSessionService.getByContentId(request);

      then(loadUserPort).should(never()).getUserSummary(any());
      ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
      then(loadUserPort).should().getUserSummaries(captor.capture());
      assertThat(captor.getValue()).containsExactly(watcherId);
    }

    @Test
    @DisplayName("배치 조회 결과에 없는 watcherId는 watcher가 null로 채워진다 (페이지 전체가 죽지 않음)")
    void missingWatcherResultsInNullWatcher() {
      UUID contentId = UUID.randomUUID();
      UUID watcherId = UUID.randomUUID();
      WatchingSessionSearchRequest request = makeRequest(contentId, 5);
      WatchingSession session = WatchingSession.create(watcherId, contentId);

      given(watchingSessionRepository.findByContentId(request)).willReturn(List.of(session));
      given(loadContentPort.getContent(contentId)).willReturn(makeContent(contentId));
      given(loadUserPort.getUserSummaries(anyCollection())).willReturn(Map.of()); // 탈퇴 등으로 없음
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(1L);

      CursorPageResponse<WatchingSessionDto> response = watchingSessionService.getByContentId(request);

      assertThat(response.data()).hasSize(1);
      assertThat(response.data().get(0).watcher()).isNull();
    }
  }
}
