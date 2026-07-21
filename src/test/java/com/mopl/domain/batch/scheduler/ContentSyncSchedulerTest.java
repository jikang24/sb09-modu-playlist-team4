package com.mopl.domain.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

@ExtendWith(MockitoExtension.class)
class ContentSyncSchedulerTest {

  private static final String JOB_NAME = "contentSyncJob";

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private JobExplorer jobExplorer;

  @Mock
  private Job contentSyncJob;

  @InjectMocks
  private ContentSyncScheduler contentSyncScheduler;

  private JobInstance jobInstance(long id) {
    return new JobInstance(id, JOB_NAME);
  }

  private JobExecution completedExecutionEndedAt(JobInstance instance, LocalDateTime endTime) {
    JobExecution execution = new JobExecution(instance, new JobParameters());
    execution.setStatus(BatchStatus.COMPLETED);
    execution.setEndTime(endTime);
    return execution;
  }

  @Test
  @DisplayName("정상 실행 - 최근 성공 기록이 없으면 timestamp 파라미터와 함께 실행한다")
  void success() throws Exception {
    given(contentSyncJob.getName()).willReturn(JOB_NAME);
    given(jobExplorer.getJobInstances(eq(JOB_NAME), eq(0), anyInt())).willReturn(List.of());

    contentSyncScheduler.runContentSyncJob();

    ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
    then(jobLauncher).should().run(eq(contentSyncJob), paramsCaptor.capture());
    assertThatCode(() -> paramsCaptor.getValue().getLong("timestamp")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("최근 실행 기록이 하나도 없어도(최초 기동 등) 성공 기록 없음으로 취급해 실행한다")
  void noJobHistory_stillRuns() throws Exception {
    given(contentSyncJob.getName()).willReturn(JOB_NAME);
    given(jobExplorer.getJobInstances(eq(JOB_NAME), eq(0), anyInt())).willReturn(List.of());

    contentSyncScheduler.runContentSyncJob();

    then(jobLauncher).should().run(eq(contentSyncJob), any());
  }

  @Test
  @DisplayName("최근 6시간 안에 성공한 실행이 있으면 스킵하고 Job을 실행하지 않는다")
  void hasRecentSuccess_skipsExecution() throws Exception {
    given(contentSyncJob.getName()).willReturn(JOB_NAME);
    JobInstance instance = jobInstance(1L);
    JobExecution recentSuccess = completedExecutionEndedAt(instance, LocalDateTime.now().minusHours(1));
    given(jobExplorer.getJobInstances(eq(JOB_NAME), eq(0), anyInt())).willReturn(List.of(instance));
    given(jobExplorer.getJobExecutions(instance)).willReturn(List.of(recentSuccess));

    contentSyncScheduler.runContentSyncJob();

    then(jobLauncher).should(never()).run(any(), any());
  }

  @Test
  @DisplayName("성공 기록은 있지만 6시간보다 오래됐으면 다시 실행한다")
  void successOlderThanFreshnessWindow_runsAgain() throws Exception {
    given(contentSyncJob.getName()).willReturn(JOB_NAME);
    JobInstance instance = jobInstance(1L);
    JobExecution staleSuccess = completedExecutionEndedAt(instance, LocalDateTime.now().minusHours(7));
    given(jobExplorer.getJobInstances(eq(JOB_NAME), eq(0), anyInt())).willReturn(List.of(instance));
    given(jobExplorer.getJobExecutions(instance)).willReturn(List.of(staleSuccess));

    contentSyncScheduler.runContentSyncJob();

    then(jobLauncher).should().run(eq(contentSyncJob), any());
  }

  @Test
  @DisplayName("최근 실행이 실패(COMPLETED 아님)만 있으면 성공 기록 없음으로 보고 다시 실행한다")
  void onlyFailedRecentExecution_runsAgain() throws Exception {
    given(contentSyncJob.getName()).willReturn(JOB_NAME);
    JobInstance instance = jobInstance(1L);
    JobExecution failed = new JobExecution(instance, new JobParameters());
    failed.setStatus(BatchStatus.FAILED);
    failed.setEndTime(LocalDateTime.now().minusMinutes(10));
    given(jobExplorer.getJobInstances(eq(JOB_NAME), eq(0), anyInt())).willReturn(List.of(instance));
    given(jobExplorer.getJobExecutions(instance)).willReturn(List.of(failed));

    contentSyncScheduler.runContentSyncJob();

    then(jobLauncher).should().run(eq(contentSyncJob), any());
  }

  @Test
  @DisplayName("호출할 때마다 timestamp 파라미터가 달라진다 (같은 파라미터로는 재실행 불가한 제약 회피용)")
  void usesUniqueTimestampPerRun() throws Exception {
    given(contentSyncJob.getName()).willReturn(JOB_NAME);
    given(jobExplorer.getJobInstances(eq(JOB_NAME), eq(0), anyInt())).willReturn(List.of());

    contentSyncScheduler.runContentSyncJob();
    Thread.sleep(2); // System.currentTimeMillis() 해상도 고려해 살짝 텀을 둠
    contentSyncScheduler.runContentSyncJob();

    ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
    then(jobLauncher).should(org.mockito.Mockito.times(2))
        .run(eq(contentSyncJob), paramsCaptor.capture());

    Long first = paramsCaptor.getAllValues().get(0).getLong("timestamp");
    Long second = paramsCaptor.getAllValues().get(1).getLong("timestamp");
    org.assertj.core.api.Assertions.assertThat(second).isNotEqualTo(first);
  }

  @Test
  @DisplayName("Job 실행 중 예외가 나도 스케줄러 자체는 예외를 던지지 않는다 (다음 스케줄에 영향 없음)")
  void jobFailure_doesNotPropagateException() throws Exception {
    given(contentSyncJob.getName()).willReturn(JOB_NAME);
    given(jobExplorer.getJobInstances(eq(JOB_NAME), eq(0), anyInt())).willReturn(List.of());
    given(jobLauncher.run(any(), any()))
        .willThrow(new JobExecutionAlreadyRunningException("이미 실행 중"));

    assertThatCode(() -> contentSyncScheduler.runContentSyncJob())
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Job 실행 성공 시 예외 없이 정상 종료된다")
  void success_completesWithoutException() throws Exception {
    given(contentSyncJob.getName()).willReturn(JOB_NAME);
    given(jobExplorer.getJobInstances(eq(JOB_NAME), eq(0), anyInt())).willReturn(List.of());
    given(jobLauncher.run(any(), any()))
        .willReturn(org.mockito.Mockito.mock(JobExecution.class));

    assertThatCode(() -> contentSyncScheduler.runContentSyncJob())
        .doesNotThrowAnyException();
  }
}
