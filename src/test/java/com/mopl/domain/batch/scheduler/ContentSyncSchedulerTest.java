package com.mopl.domain.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

@ExtendWith(MockitoExtension.class)
class ContentSyncSchedulerTest {

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job contentSyncJob;

  @InjectMocks
  private ContentSyncScheduler contentSyncScheduler;

  @Test
  @DisplayName("정상 실행 - contentSyncJob을 timestamp 파라미터와 함께 실행한다")
  void success() throws Exception {
    contentSyncScheduler.runContentSyncJob();

    ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
    then(jobLauncher).should().run(eq(contentSyncJob), paramsCaptor.capture());
    assertThatCode(() -> paramsCaptor.getValue().getLong("timestamp")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("호출할 때마다 timestamp 파라미터가 달라진다 (중복 실행 방지용)")
  void usesUniqueTimestampPerRun() throws Exception {
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
    given(jobLauncher.run(any(), any()))
        .willThrow(new JobExecutionAlreadyRunningException("이미 실행 중"));

    assertThatCode(() -> contentSyncScheduler.runContentSyncJob())
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Job 실행 성공 시 예외 없이 정상 종료된다")
  void success_completesWithoutException() throws Exception {
    given(jobLauncher.run(any(), any()))
        .willReturn(org.mockito.Mockito.mock(JobExecution.class));

    assertThatCode(() -> contentSyncScheduler.runContentSyncJob())
        .doesNotThrowAnyException();
  }
}
