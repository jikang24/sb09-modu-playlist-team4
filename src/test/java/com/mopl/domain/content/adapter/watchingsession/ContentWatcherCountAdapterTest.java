package com.mopl.domain.content.adapter.watchingsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.mopl.domain.watchingsession.service.WatchingSessionCountService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ContentWatcherCountAdapter는 WatchingSessionCountService에 단순 위임만 하므로
 * (중복 제거 등 실제 로직은 WatchingSessionCountServiceTest에서 검증됨) 위임 자체만 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class ContentWatcherCountAdapterTest {

  @Mock
  private WatchingSessionCountService watchingSessionCountService;

  @InjectMocks
  private ContentWatcherCountAdapter adapter;

  @Test
  @DisplayName("countByContentId: WatchingSessionCountService 조회 결과를 그대로 반환한다")
  void countByContentId_delegatesToService() {
    UUID contentId = UUID.randomUUID();
    given(watchingSessionCountService.countByContentId(contentId)).willReturn(5L);

    long count = adapter.countByContentId(contentId);

    assertThat(count).isEqualTo(5L);
  }

  @Test
  @DisplayName("countByContentIds: WatchingSessionCountService 배치 조회 결과를 그대로 반환한다")
  void countByContentIds_delegatesToService() {
    UUID contentId1 = UUID.randomUUID();
    UUID contentId2 = UUID.randomUUID();
    List<UUID> contentIds = List.of(contentId1, contentId2);
    given(watchingSessionCountService.countByContentIds(contentIds))
        .willReturn(Map.of(contentId1, 2L, contentId2, 0L));

    Map<UUID, Long> counts = adapter.countByContentIds(contentIds);

    assertThat(counts).containsEntry(contentId1, 2L).containsEntry(contentId2, 0L);
  }
}