package com.mopl.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.review.adapter.port.LoadUserPort;
import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.global.dto.UserSummary;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * updateReview는 previousRating을 findById로 읽은 뒤 delta를 계산해 이벤트를 발행하는데,
 * Review 엔티티에 락/버전 체크가 없어 같은 리뷰를 거의 동시에 두 번 수정하면(더블클릭/재전송 등)
 * 두 요청 모두 같은 previousRating을 보고 각자 delta를 계산 - applyRatingDelta에 delta가
 * 두 번 반영돼 review_count는 그대로인데 average_rating만 부풀어 오르는지 검증한다.
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
    "opensearch.init.enabled=false",
    "redis.listener.enabled=false",
    "MAIL_USERNAME=test@test.com",
    "MAIL_PASSWORD=test"
})
class ReviewUpdateConcurrencyIntegrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("mopl")
          .withUsername("postgres")
          .withPassword("postgres");

  @Container
  @ServiceConnection
  static GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine")
          .withExposedPorts(6379);

  @Autowired
  private ReviewService reviewService;

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @MockitoBean
  private LoadUserPort loadUserPort;

  private UUID contentId;
  private UUID reviewId;

  @AfterEach
  void tearDown() {
    if (reviewId != null) {
      reviewRepository.findById(reviewId).ifPresent(reviewRepository::delete);
    }
    if (contentId != null) {
      contentRepository.deleteById(contentId);
    }
  }

  @Test
  @DisplayName("같은 리뷰를 동시에 두 번 수정하면 review_count는 그대로인데 average_rating이 이중 반영된다")
  void concurrentUpdate_doubleCountsRatingDelta() throws InterruptedException {
    UUID userId = UUID.randomUUID();
    given(loadUserPort.getUserSummary(userId))
        .willReturn(new UserSummary(userId, "테스트유저", null));

    Content content = contentRepository.save(Content.create(
        ContentType.MOVIE,
        Content.MANUAL_EXTERNAL_ID_PREFIX + UUID.randomUUID(),
        "테스트 제목", "테스트 설명", null, List.of()
    ));
    contentId = content.getId();

    reviewId = reviewService.createReview(contentId, userId, new BigDecimal("3.0"), "초기 리뷰");

    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
        txTemplate.executeWithoutResult(status -> {
          Content updated = contentRepository.findById(contentId).orElseThrow();
          assertThat(updated.getReviewCount()).isEqualTo(1);
          assertThat(updated.getAverageRating()).isEqualByComparingTo("3.0");
        }));

    // 3.0 -> 5.0으로 동시에 두 번 수정 (더블클릭/재전송 재현)
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);

    Runnable updateTask = () -> {
      ready.countDown();
      try {
        go.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      reviewService.updateReview(reviewId, userId, new BigDecimal("5.0"), "수정된 리뷰");
    };

    pool.submit(updateTask);
    pool.submit(updateTask);
    ready.await();
    go.countDown();
    pool.shutdown();
    pool.awaitTermination(5, TimeUnit.SECONDS);

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
        txTemplate.executeWithoutResult(status -> {
          Content updated = contentRepository.findById(contentId).orElseThrow();
          assertThat(updated.getReviewCount()).isEqualTo(1);
          // 정상이라면 리뷰가 1개뿐이니 평균은 그 리뷰의 최종 rating과 같아야 한다 (5.0)
          assertThat(updated.getAverageRating()).isEqualByComparingTo("5.0");
        }));
  }
}