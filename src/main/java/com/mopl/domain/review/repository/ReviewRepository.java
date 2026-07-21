package com.mopl.domain.review.repository;

import com.mopl.domain.review.domain.Review;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository
    extends JpaRepository<Review, UUID>, ReviewRepositoryCustom {

  // 1인 1리뷰 검증용 (Spring Data가 메서드 이름만 보고 쿼리 자동 생성)
  boolean existsByContentIdAndUserId(UUID contentId, UUID userId);

  // User 프로필이 바뀌었을 때, 이 유저가 쓴 "모든" 리뷰를 한 번의 UPDATE 쿼리로 일괄 갱신
  @Modifying
  @Query("UPDATE Review r SET r.authorName = :name, r.authorProfileImageUrl = :imageUrl "
      + "WHERE r.userId = :userId")
  void updateAuthorSnapshotByUserId(
      @Param("userId") UUID userId,
      @Param("name") String name,
      @Param("imageUrl") String imageUrl);

  // 콘텐츠가 삭제됐을 때, FK 없이 contentId만 참조하는 리뷰가 고아로 남지 않도록 일괄 삭제
  @Modifying
  @Query("DELETE FROM Review r WHERE r.contentId = :contentId")
  void deleteByContentId(@Param("contentId") UUID contentId);
}