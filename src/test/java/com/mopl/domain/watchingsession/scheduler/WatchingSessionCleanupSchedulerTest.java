package com.mopl.domain.watchingsession.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchingSessionCleanupScheduler 테스트")
class WatchingSessionCleanupSchedulerTest {

  @Mock
  private WatchingSessionRepository watchingSessionRepository;

  @InjectMocks
  private WatchingSessionCleanupScheduler scheduler;

  @Test
  @DisplayName("정리 배치 실행 시 저장소의 고아 세션 정리를 호출한다")
  void cleanup_delegatesToRepository() {
    given(watchingSessionRepository.cleanupOrphanSessions()).willReturn(3L);

    scheduler.cleanup();

    then(watchingSessionRepository).should().cleanupOrphanSessions();
  }
}