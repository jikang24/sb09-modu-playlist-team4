package com.mopl.domain.batch.controller;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
public class BatchController {

  private final JobLauncher jobLauncher;
  private final Job contentSyncJob;

  @PostMapping("/content-sync")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<String>> runContentSyncJob() {
    try {
      JobParameters params = new JobParametersBuilder()
          .addLong("timestamp", System.currentTimeMillis()) // 매번 다른 파라미터 (중복 방지)
          .toJobParameters();

      jobLauncher.run(contentSyncJob, params);
      log.info("[Batch] contentSyncJob 수동 실행");

      return ResponseEntity.ok(ApiResponse.ok("콘텐츠 수집 Job 실행 완료"));
    } catch (Exception e) {
      log.error("[Batch] contentSyncJob 실행 실패 - {}", e.getMessage());
      throw new MoplException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }
}