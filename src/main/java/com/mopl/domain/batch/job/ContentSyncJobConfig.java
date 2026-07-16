package com.mopl.domain.batch.job;

import com.mopl.domain.batch.service.ContentSyncService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 콘텐츠 수집 Batch Job 설정
 *
 * 실행 방법:
 * 1. 수동 실행: POST /api/admin/batch/content-sync
 * 2. 스케줄 실행: ContentSyncScheduler (앱 기동 직후 1회 + 6시간마다 자동 실행)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContentSyncJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final ContentSyncService contentSyncService;
  private final MeterRegistry meterRegistry;

  // TMDB popular 엔드포인트는 페이지당 20건, 최대 500페이지(=10,000건)까지 제공
  // 원래 목표는 영화 250페이지(5,000건) + TV 250페이지(5,000건) = 총 10,000건이었으나,
  // ECS 배포 직후 헬스체크 실패 이슈로 일시적으로 절반(125+125=5,000건)까지 낮춰둠.
  // upsert라 기존 데이터가 지워지진 않으니, 안정화되면 다시 250/250으로 복원하면 됨.
  private static final int MOVIE_SYNC_PAGE_COUNT = 125;
  private static final int TV_SYNC_PAGE_COUNT = 125;

  // 페이지 조회를 동시에 몇 개까지 보낼지 (TMDB rate limit 여유 + ECS 배포 직후 헬스체크 실패를
  // 유발하던 DB 커넥션 풀 경합 완화를 위해 8에서 낮춤. ShedLock의 lockAtMostFor(30분) 안에는
  // 최악의 재시도 케이스로도 충분히 여유 있게 끝남)
  private static final int PAGE_FETCH_CONCURRENCY = 4;

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
    return createPagedSyncStep("movieSyncStep", "영화", MOVIE_SYNC_PAGE_COUNT, page ->
        contentSyncService.syncMovies(page));
  }

  @Bean
  public Step tvSeriesSyncStep() {
    return createPagedSyncStep("tvSeriesSyncStep", "TV시리즈", TV_SYNC_PAGE_COUNT, page ->
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
   * @param stepName   Step 이름 (Batch 메타 테이블에 저장됨)
   * @param typeName   로그용 콘텐츠 타입 이름
   * @param pageCount  수집할 페이지 수
   * @param syncTask   페이지 번호를 받아 수집 후 저장 건수를 반환하는 함수
   */
  private Step createPagedSyncStep(String stepName, String typeName, int pageCount,
      PageSyncTask syncTask) {
    return new StepBuilder(stepName, jobRepository)
        .tasklet((contribution, chunkContext) -> {
          log.info("[Batch] {} 수집 시작 - {}페이지 (동시 {}개)",
              typeName, pageCount, PAGE_FETCH_CONCURRENCY);

          ExecutorService executor = Executors.newFixedThreadPool(PAGE_FETCH_CONCURRENCY);
          try {
            List<Future<Integer>> futures = new ArrayList<>(pageCount);
            for (int page = 1; page <= pageCount; page++) {
              int currentPage = page;
              futures.add(executor.submit(() -> {
                try {
                  int saved = syncTask.sync(currentPage);
                  log.info("[Batch] {} {}페이지 완료 - 저장: {}건",
                      typeName, currentPage, saved);
                  return saved;
                } catch (DataAccessException e) {
                  // 동시에 처리된 다른 페이지와 externalId가 겹쳐 유니크 제약 충돌이 나는 경우
                  // (TMDB 인기 순위가 수집 도중 바뀌며 같은 항목이 여러 페이지에 걸칠 때 발생 가능).
                  // 이 페이지만 건너뛰고 다음 배치 회차에서 자연히 재수집하면서 전체 Step은 살린다.
                  log.warn("[Batch] {} {}페이지 저장 중 DB 충돌 - 스킵: {}",
                      typeName, currentPage, e.getMessage());
                  meterRegistry.counter("content.sync.page.skipped", "step", stepName).increment();
                  return 0;
                }
              }));
            }

            int totalSaved = 0;
            for (Future<Integer> future : futures) {
              totalSaved += future.get();
            }

            log.info("[Batch] {} 수집 완료 - 총 저장: {}건", typeName, totalSaved);
            return RepeatStatus.FINISHED;
          } catch (ExecutionException e) {
            throw new IllegalStateException(
                "[Batch] " + typeName + " 페이지 수집 실패", e.getCause());
          } finally {
            // 실패로 빠져나온 경우 이미 제출된 나머지 페이지 작업이 백그라운드에서
            // 계속 돌지 않도록 즉시 취소한다 (shutdown()은 이미 큐/실행 중인 작업을 끝까지 진행시킴)
            executor.shutdownNow();
          }
        }, transactionManager)
        .build();
  }

  @FunctionalInterface
  private interface PageSyncTask {
    int sync(int page);
  }
}