package com.mopl.domain.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.mopl.domain.batch.service.ContentSyncService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Step은 @Bean 메서드 안에 Tasklet이 람다로 정의돼 있어, Spring Batch를 실제로 띄우는 대신
 * TaskletStep에서 Tasklet을 직접 꺼내 execute()를 호출하는 방식으로 검증한다.
 * (contribution/chunkContext는 이 Tasklet 로직에서 쓰지 않아 mock으로 충분)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentSyncJobConfig 테스트")
class ContentSyncJobConfigTest {

  @Mock
  private JobRepository jobRepository;

  @Mock
  private PlatformTransactionManager transactionManager;

  @Mock
  private ContentSyncService contentSyncService;

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

  private ContentSyncJobConfig contentSyncJobConfig;

  @BeforeEach
  void setUp() {
    contentSyncJobConfig =
        new ContentSyncJobConfig(jobRepository, transactionManager, contentSyncService, meterRegistry);
  }

  private RepeatStatus runTasklet(Step step) throws Exception {
    Tasklet tasklet = ((TaskletStep) step).getTasklet();
    return tasklet.execute(mock(StepContribution.class), mock(ChunkContext.class));
  }

  @Nested
  @DisplayName("contentSyncJob")
  class ContentSyncJobBean {

    @Test
    @DisplayName("영화/TV/스포츠 3개 Step으로 구성된 Job을 생성한다")
    void job_isCreatedWithExpectedName() {
      Job job = contentSyncJobConfig.contentSyncJob();

      assertThat(job.getName()).isEqualTo("contentSyncJob");
    }
  }

  @Nested
  @DisplayName("movieSyncStep")
  class MovieSyncStep {

    @Test
    @DisplayName("125페이지를 전부 조회해서 합산 저장 건수를 반환하고 정상 완료한다")
    void success_aggregatesSavedCountAcrossAllPages() throws Exception {
      given(contentSyncService.syncMovies(anyInt())).willReturn(1);

      RepeatStatus status = runTasklet(contentSyncJobConfig.movieSyncStep());

      assertThat(status).isEqualTo(RepeatStatus.FINISHED);
      then(contentSyncService).should(times(125)).syncMovies(anyInt());
    }

    @Test
    @DisplayName("특정 페이지에서 DB 유니크 제약 충돌이 나면 그 페이지만 스킵하고 메트릭을 남기되 Step은 정상 완료한다")
    void dataAccessException_skipsPageAndRecordsMetric_withoutFailingStep() throws Exception {
      given(contentSyncService.syncMovies(anyInt())).willReturn(1);
      given(contentSyncService.syncMovies(eq(3)))
          .willThrow(new DataIntegrityViolationException("unique constraint violated"));

      RepeatStatus status = runTasklet(contentSyncJobConfig.movieSyncStep());

      assertThat(status).isEqualTo(RepeatStatus.FINISHED);
      assertThat(meterRegistry.counter("content.sync.page.skipped", "step", "movieSyncStep").count())
          .isEqualTo(1.0);
    }

    @Test
    @DisplayName("DB 충돌이 아닌 예외는 페이지 단위로 삼켜지지 않고 Step 전체 실패로 전파된다")
    void nonDataAccessException_propagatesAsStepFailure() {
      given(contentSyncService.syncMovies(anyInt())).willReturn(1);
      given(contentSyncService.syncMovies(eq(7)))
          .willThrow(new MoplException(ErrorCode.TMDB_CLIENT_ERROR));

      Step movieSyncStep = contentSyncJobConfig.movieSyncStep();

      assertThatThrownBy(() -> runTasklet(movieSyncStep))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("영화")
          .hasCauseInstanceOf(MoplException.class);
    }
  }

  @Nested
  @DisplayName("tvSeriesSyncStep")
  class TvSeriesSyncStep {

    @Test
    @DisplayName("125페이지를 전부 조회해서 합산 저장 건수를 반환하고 정상 완료한다")
    void success_aggregatesSavedCountAcrossAllPages() throws Exception {
      given(contentSyncService.syncTvSeries(anyInt())).willReturn(1);

      RepeatStatus status = runTasklet(contentSyncJobConfig.tvSeriesSyncStep());

      assertThat(status).isEqualTo(RepeatStatus.FINISHED);
      then(contentSyncService).should(times(125)).syncTvSeries(anyInt());
    }

    @Test
    @DisplayName("특정 페이지에서 DB 유니크 제약 충돌이 나면 그 페이지만 스킵하고 메트릭을 남긴다")
    void dataAccessException_skipsPageAndRecordsMetric() throws Exception {
      given(contentSyncService.syncTvSeries(anyInt())).willReturn(1);
      given(contentSyncService.syncTvSeries(eq(5)))
          .willThrow(new DataIntegrityViolationException("unique constraint violated"));

      RepeatStatus status = runTasklet(contentSyncJobConfig.tvSeriesSyncStep());

      assertThat(status).isEqualTo(RepeatStatus.FINISHED);
      assertThat(meterRegistry.counter("content.sync.page.skipped", "step", "tvSeriesSyncStep").count())
          .isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("sportsSyncStep")
  class SportsSyncStep {

    @Test
    @DisplayName("스포츠 이벤트를 수집해 저장하고 정상 완료한다")
    void success() throws Exception {
      given(contentSyncService.syncSportsEvents()).willReturn(42);

      RepeatStatus status = runTasklet(contentSyncJobConfig.sportsSyncStep());

      assertThat(status).isEqualTo(RepeatStatus.FINISHED);
      then(contentSyncService).should().syncSportsEvents();
    }
  }
}