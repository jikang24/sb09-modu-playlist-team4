package com.mopl.domain.playlist.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistJpaRepository extends JpaRepository<PlaylistJpaEntity, UUID>,
    JpaSpecificationExecutor<PlaylistJpaEntity>,
    PlaylistRepositoryCustom {

  /**
   * 콘텐츠 추가/삭제/메타데이터 수정처럼 "조회한 스냅샷을 그대로 다시 저장"하는 흐름 전용.
   * 행 잠금 없이 조회하면, 두 요청이 겹칠 때 뒤에 커밋되는 쪽의 스냅샷이 앞쪽이 추가한
   * 콘텐츠를 모른 채 저장되어 PlaylistPersistenceAdapter.syncContents()가 그걸 고아로
   * 취급해 지워버린다 (lost update). SELECT ... FOR UPDATE로 같은 playlist 행에 대한
   * 동시 수정을 트랜잭션 단위로 직렬화해서 막는다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PlaylistJpaEntity p where p.id = :id")
  Optional<PlaylistJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
