package com.mopl.domain.batch.job;

import com.mopl.domain.batch.service.ContentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 콘텐츠 수집 Batch Job 설정
 *
 * 실행 방법:
 * 1. 수동 실행: POST /api/admin/batch/content-sync
 * 2. 스케줄 실행: @Scheduled (추후 추가) TODO
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContentSyncJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final ContentSyncService contentSyncService;

  private static final int SYNC_PAGE_COUNT = 5;

  @Bean
  public Job contentSyncJob() {
    return new JobBuilder("contentSyncJob", jobRepository)
        .start(movieSyncStep())
        .next(tvSeriesSyncStep())
        .next(sportsSyncStep())
        .build();
  }

  @Bean
  public Step movieSyncStep() {
    return new StepBuilder("movieSyncStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          int totalSaved = 0;
          for (int page = 1; page <= SYNC_PAGE_COUNT; page++) {
            totalSaved += contentSyncService.syncMovies(page);
          }
          log.info("[Batch] 영화 Step 완료 - 총 저장: {}건", totalSaved);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  @Bean
  public Step tvSeriesSyncStep() {
    return new StepBuilder("tvSeriesSyncStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          int totalSaved = 0;
          for (int page = 1; page <= SYNC_PAGE_COUNT; page++) {
            totalSaved += contentSyncService.syncTvSeries(page);
          }
          log.info("[Batch] TV시리즈 Step 완료 - 총 저장: {}건", totalSaved);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  @Bean
  public Step sportsSyncStep() {
    return new StepBuilder("sportsSyncStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          int totalSaved = contentSyncService.syncSportsEvents();
          log.info("[Batch] 스포츠 Step 완료 - 총 저장: {}건", totalSaved);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }
}