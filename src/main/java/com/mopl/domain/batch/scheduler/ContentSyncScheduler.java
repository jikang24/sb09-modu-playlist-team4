package com.mopl.domain.batch.scheduler;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 콘텐츠 수집 Job 자동 실행
 *
 * BatchController의 수동 트리거(POST /api/admin/batch/content-sync)와는 별개로,
 * 앱 기동 직후 1회 + 이후 주기적으로 자동 실행되어 매번 수동으로 배치를 트리거하지 않아도 됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentSyncScheduler {

  private final JobLauncher jobLauncher;
  private final Job contentSyncJob;

  @Scheduled(initialDelay = 0, fixedRate = 6, timeUnit = TimeUnit.HOURS)
  public void runContentSyncJob() {
    try {
      JobParameters params = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis()) // 매번 다른 파라미터 (중복 방지)
          .toJobParameters();

      jobLauncher.run(contentSyncJob, params);
      log.info("[Batch] contentSyncJob 스케줄 실행 완료");
    } catch (Exception e) {
      log.error("[Batch] contentSyncJob 스케줄 실행 실패 - {}", e.getMessage(), e);
    }
  }
}
