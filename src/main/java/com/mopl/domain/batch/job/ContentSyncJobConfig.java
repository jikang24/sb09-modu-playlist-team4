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
    return createPagedSyncStep("movieSyncStep", "영화", page ->
        contentSyncService.syncMovies(page));
  }

  @Bean
  public Step tvSeriesSyncStep() {
    return createPagedSyncStep("tvSeriesSyncStep", "TV시리즈", page ->
        contentSyncService.syncTvSeries(page));
  }

  //SportsDB는 별도 외부 API라 개별로 작업
  @Bean
  public Step sportsSyncStep() {
    return new StepBuilder("sportsSyncStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          log.info("[Batch] 스포츠 수집 시작");
          int saved = contentSyncService.syncSportsEvents();
          log.info("[Batch] 스포츠 수집 완료 - 총 저장: {}건", saved);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  /**
   * @param stepName  Step 이름 (Batch 메타 테이블에 저장됨)
   * @param typeName  로그용 콘텐츠 타입 이름
   * @param syncTask  페이지 번호를 받아 수집 후 저장 건수를 반환하는 함수
   */
  private Step createPagedSyncStep(String stepName, String typeName,
      PageSyncTask syncTask) {
    return new StepBuilder(stepName, jobRepository)
        .tasklet((contribution, chunkContext) -> {
          log.info("[Batch] {} 수집 시작 - {}페이지", typeName, SYNC_PAGE_COUNT);
          int totalSaved = 0;

          for (int page = 1; page <= SYNC_PAGE_COUNT; page++) {
            int saved = syncTask.sync(page);
            totalSaved += saved;
            log.info("[Batch] {} {}페이지 완료 - 저장: {}건",
                typeName, page, saved);
          }

          log.info("[Batch] {} 수집 완료 - 총 저장: {}건", typeName, totalSaved);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  @FunctionalInterface
  private interface PageSyncTask {
    int sync(int page);
  }
}