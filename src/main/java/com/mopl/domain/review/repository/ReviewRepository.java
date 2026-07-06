package com.mopl.domain.review.repository;

import com.mopl.domain.review.domain.Review;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository
    extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {

  // 1인 1리뷰 검증용 (Spring Data가 메서드 이름만 보고 쿼리 자동 생성)
  boolean existsByContentIdAndUserId(UUID contentId, UUID userId);

  // 콘텐츠 하나의 평균 평점 + 리뷰 개수를 한 번의 쿼리로 계산
  // AVG는 리뷰가 0개면 null, COUNT는 0 - Service에서 null 처리함
  @Query("SELECT AVG(r.rating), COUNT(r) FROM Review r WHERE r.contentId = :contentId")
  Object[] aggregateRatingStats(@Param("contentId") UUID contentId);

  // User 프로필이 바뀌었을 때, 이 유저가 쓴 "모든" 리뷰를 한 번의 UPDATE 쿼리로 일괄 갱신
  @Modifying
  @Query("UPDATE Review r SET r.authorName = :name, r.authorProfileImageUrl = :imageUrl "
      + "WHERE r.userId = :userId")
  void updateAuthorSnapshotByUserId(
      @Param("userId") UUID userId,
      @Param("name") String name,
      @Param("imageUrl") String imageUrl);
}