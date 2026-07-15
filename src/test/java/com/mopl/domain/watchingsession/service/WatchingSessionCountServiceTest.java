package com.mopl.domain.watchingsession.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchingSessionCountServiceTest {

  @Mock
  private WatchingSessionRepository watchingSessionRepository;

  @InjectMocks
  private WatchingSessionCountService watchingSessionCountService;

  @Test
  @DisplayName("countByContentId: 콘텐츠 하나의 실시간 시청자 수를 반환한다")
  void countByContentId_success() {
    UUID contentId = UUID.randomUUID();
    given(watchingSessionRepository.countByContentId(contentId)).willReturn(7L);

    long count = watchingSessionCountService.countByContentId(contentId);

    assertThat(count).isEqualTo(7L);
  }

  @Test
  @DisplayName("countByContentIds: 콘텐츠별 시청자 수를 한 번에 조회한다")
  void countByContentIds_success() {
    UUID contentId1 = UUID.randomUUID();
    UUID contentId2 = UUID.randomUUID();
    given(watchingSessionRepository.countByContentId(contentId1)).willReturn(2L);
    given(watchingSessionRepository.countByContentId(contentId2)).willReturn(0L);

    Map<UUID, Long> counts =
        watchingSessionCountService.countByContentIds(List.of(contentId1, contentId2));

    assertThat(counts).containsEntry(contentId1, 2L).containsEntry(contentId2, 0L);
  }

  @Test
  @DisplayName("countByContentIds: 중복된 contentId는 한 번만 조회한다")
  void countByContentIds_dedupesDuplicateIds() {
    UUID contentId = UUID.randomUUID();
    given(watchingSessionRepository.countByContentId(contentId)).willReturn(3L);

    Map<UUID, Long> counts =
        watchingSessionCountService.countByContentIds(List.of(contentId, contentId));

    assertThat(counts).containsExactly(Map.entry(contentId, 3L));
    then(watchingSessionRepository).should(times(1)).countByContentId(contentId);
  }

  @Test
  @DisplayName("countByContentIds: 빈 컬렉션이면 빈 맵을 반환한다")
  void countByContentIds_emptyInput() {
    Map<UUID, Long> counts = watchingSessionCountService.countByContentIds(List.of());

    assertThat(counts).isEmpty();
    then(watchingSessionRepository).should(never()).countByContentId(any());
  }
}