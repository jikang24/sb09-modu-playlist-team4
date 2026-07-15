package com.mopl.domain.content.adapter.watchingsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentWatcherCountAdapterTest {

  @Mock
  private WatchingSessionRepository watchingSessionRepository;

  @InjectMocks
  private ContentWatcherCountAdapter adapter;

  @Nested
  @DisplayName("countByContentId()")
  class CountByContentId {

    @Test
    @DisplayName("리포지토리 조회 결과를 그대로 반환한다")
    void returnsRepositoryResult() {
      UUID contentId = UUID.randomUUID();
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(5L);

      long count = adapter.countByContentId(contentId);

      assertThat(count).isEqualTo(5L);
    }
  }

  @Nested
  @DisplayName("countByContentIds()")
  class CountByContentIds {

    @Test
    @DisplayName("콘텐츠별 시청자 수를 배치로 조회해 맵으로 반환한다")
    void returnsCountsPerContentId() {
      UUID contentId1 = UUID.randomUUID();
      UUID contentId2 = UUID.randomUUID();
      given(watchingSessionRepository.countByContentId(contentId1)).willReturn(2L);
      given(watchingSessionRepository.countByContentId(contentId2)).willReturn(0L);

      Map<UUID, Long> counts = adapter.countByContentIds(List.of(contentId1, contentId2));

      assertThat(counts).containsEntry(contentId1, 2L).containsEntry(contentId2, 0L);
    }

    @Test
    @DisplayName("중복된 contentId는 한 번만 조회한다")
    void dedupesDuplicateIds() {
      UUID contentId = UUID.randomUUID();
      given(watchingSessionRepository.countByContentId(contentId)).willReturn(3L);

      Map<UUID, Long> counts = adapter.countByContentIds(List.of(contentId, contentId));

      assertThat(counts).containsExactly(Map.entry(contentId, 3L));
      then(watchingSessionRepository).should(org.mockito.Mockito.times(1)).countByContentId(contentId);
    }

    @Test
    @DisplayName("빈 컬렉션이면 빈 맵을 반환한다")
    void emptyInput_returnsEmptyMap() {
      Map<UUID, Long> counts = adapter.countByContentIds(List.of());

      assertThat(counts).isEmpty();
    }
  }
}