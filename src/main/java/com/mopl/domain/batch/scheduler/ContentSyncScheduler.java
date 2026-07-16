package com.mopl.domain.batch.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 콘텐츠 수집 Job 자동 실행
 *
 * BatchController의 수동 트리거(POST /api/admin/batch/content-sync)와는 별개로,
 * 앱 기동 1분 후부터 30분마다 깨어나 "최근 SUCCESS_FRESHNESS(6시간) 안에 성공한 실행이 있는지"만
 * 가볍게 확인하고, 없을 때만 실제로 무거운 수집 Job을 실행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentSyncScheduler {

  private static final Duration SUCCESS_FRESHNESS = Duration.ofHours(6);
  // 30분마다 폴링하면서 실행이 실패해 새 JobInstance가 자꾸 쌓여도 최근 6시간 구간을
  // 충분히 훑을 수 있도록 넉넉히 잡은 조회 개수 (메타데이터 조회라 비용이 크지 않음)
  private static final int LOOKBACK_INSTANCE_COUNT = 20;

  private final JobLauncher jobLauncher;
  private final JobExplorer jobExplorer;
  private final Job contentSyncJob;

  //최초 헬스케어 시간을 위해 1분 지연, 30분마다 스케줄 체크
  @Scheduled(initialDelayString = "PT1M", fixedRateString = "PT30M")
  //스케줄 주기를 30분잡아 태스크가 죽었을때 락이 길것같아 다음 폴링까지 막는것 방지차 분산락 15분 설정
  @SchedulerLock(name = "contentSyncJob", lockAtMostFor = "PT15M")
  public void runContentSyncJob() {
    if (hasRecentSuccessfulRun()) {
      log.debug("[Batch] 최근 {}시간 안에 성공한 contentSyncJob 실행이 있어 이번 폴링은 스킵",
          SUCCESS_FRESHNESS.toHours());
      return;
    }

    try {
      JobParameters params = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis()) // 매번 다른 파라미터 (재실행 허용용)
          .toJobParameters();

      jobLauncher.run(contentSyncJob, params);
      log.info("[Batch] contentSyncJob 스케줄 실행 완료");
    } catch (Exception e) {
      log.error("[Batch] contentSyncJob 스케줄 실행 실패 - {}", e.getMessage(), e);
    }
  }

  /** 최근 실행이 하나도 없는 경우(최초 기동 등)도 "성공 기록 없음" → 실행 대상으로 자연스럽게 처리됨 */
  private boolean hasRecentSuccessfulRun() {
    LocalDateTime threshold = LocalDateTime.now().minus(SUCCESS_FRESHNESS);
    List<JobInstance> recentInstances =
        jobExplorer.getJobInstances(contentSyncJob.getName(), 0, LOOKBACK_INSTANCE_COUNT);

    return recentInstances.stream()
        .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
        .anyMatch(execution -> execution.getStatus() == BatchStatus.COMPLETED
            && execution.getEndTime() != null
            && execution.getEndTime().isAfter(threshold));
  }
}
