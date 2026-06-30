package com.mopl.domain.content.repository;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.infrastructure.ContentJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

//JpaSpecificationExecutor: 동적 쿼리(검색/필터) 지원
interface ContentJpaRepository extends JpaRepository<ContentJpaEntity, UUID>,
    JpaSpecificationExecutor<ContentJpaEntity> {

  Optional<ContentJpaEntity> findByTypeAndExternalId(ContentType type, String externalId);
}