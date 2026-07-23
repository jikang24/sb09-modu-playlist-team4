package com.mopl.domain.review.repository;

import com.mopl.domain.review.domain.Review;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository
    extends JpaRepository<Review, UUID>, ReviewRepositoryCustom {

  // 1인 1리뷰 검증용 (Spring Data가 메서드 이름만 보고 쿼리 자동 생성)
  boolean existsByContentIdAndUserId(UUID contentId, UUID userId);

  /**
   * 수정/삭제처럼 "조회한 rating을 기준으로 delta를 계산해 이벤트를 발행"하는 흐름 전용.
   * 행 잠금 없이 조회하면, 같은 리뷰에 대한 동시 요청(더블클릭/재전송)이 똑같은 이전 rating을
   * 보고 각자 delta를 계산해 content의 average_rating에 중복 반영된다 (review_count는
   * 안 늘어나는데 평점만 부풀어 오름). SELECT ... FOR UPDATE로 같은 리뷰 행에 대한 동시
   * 수정/삭제를 트랜잭션 단위로 직렬화해서, 뒤 트랜잭션이 앞 트랜잭션이 반영한 최신 rating을
   * 보고 delta를 계산하게 한다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Review r where r.id = :id")
  Optional<Review> findByIdForUpdate(@Param("id") UUID id);

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