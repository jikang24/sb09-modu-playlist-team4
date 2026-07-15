package com.mopl.domain.content.adapter.watchingsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.mopl.domain.watchingsession.service.WatchingSessionService;
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
class ContentWatcherCountAdapterTest {

  @Mock
  private WatchingSessionService watchingSessionService;

  @InjectMocks
  private ContentWatcherCountAdapter adapter;

  @Test
  @DisplayName("countByContentId: WatchingSessionService 조회 결과를 그대로 반환한다")
  void countByContentId_delegatesToService() {
    UUID contentId = UUID.randomUUID();
    given(watchingSessionService.countByContentId(contentId)).willReturn(5L);

    long count = adapter.countByContentId(contentId);

    assertThat(count).isEqualTo(5L);
  }

  @Test
  @DisplayName("countByContentIds: WatchingSessionService 배치 조회 결과를 그대로 반환한다")
  void countByContentIds_delegatesToService() {
    UUID contentId1 = UUID.randomUUID();
    UUID contentId2 = UUID.randomUUID();
    List<UUID> contentIds = List.of(contentId1, contentId2);
    given(watchingSessionService.countByContentIds(contentIds))
        .willReturn(Map.of(contentId1, 2L, contentId2, 0L));

    Map<UUID, Long> counts = adapter.countByContentIds(contentIds);

    assertThat(counts).containsEntry(contentId1, 2L).containsEntry(contentId2, 0L);
  }
}