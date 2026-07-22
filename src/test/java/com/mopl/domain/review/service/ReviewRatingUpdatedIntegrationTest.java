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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 리뷰 작성 → ReviewRatingUpdatedEvent 발행 → AFTER_COMMIT 비동기 리스너 →
 * Content.applyRatingDelta까지 실제 Spring 이벤트 버스/트랜잭션/@Async를 태워서
 * 끝까지 이어지는지 검증한다. (프로덕션에서 리뷰는 저장되는데 콘텐츠 평점이
 * 0.0으로 안 바뀌는 문제 재현/검증용)
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
    "opensearch.init.enabled=false",
    "redis.listener.enabled=false",
    "MAIL_USERNAME=test@test.com",
    "MAIL_PASSWORD=test"
})
class ReviewRatingUpdatedIntegrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("mopl")
          .withUsername("postgres")
          .withPassword("postgres");

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
  @DisplayName("리뷰를 작성하면(실제 트랜잭션 커밋 + 비동기 리스너) 콘텐츠의 평점/리뷰수가 갱신된다")
  void createReview_updatesContentRating_endToEnd() {
    UUID userId = UUID.randomUUID();
    given(loadUserPort.getUserSummary(userId))
        .willReturn(new UserSummary(userId, "테스트유저", null));

    Content content = contentRepository.save(Content.create(
        ContentType.MOVIE,
        Content.MANUAL_EXTERNAL_ID_PREFIX + UUID.randomUUID(),
        "테스트 제목", "테스트 설명", null, List.of()
    ));
    contentId = content.getId();

    reviewId = reviewService.createReview(contentId, userId, new BigDecimal("4.5"), "좋아요");

    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
        txTemplate.executeWithoutResult(status -> {
          Content updated = contentRepository.findById(contentId).orElseThrow();
          assertThat(updated.getReviewCount()).isEqualTo(1);
          assertThat(updated.getAverageRating()).isEqualByComparingTo("4.5");
        }));
  }
}