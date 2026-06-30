package com.mopl.domain.content.repository;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.infrastructure.ContentJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContentJpaRepository extends JpaRepository<ContentJpaEntity, UUID> {

  Optional<ContentJpaEntity> findByTypeAndExternalId(ContentType type, String externalId);

  List<ContentJpaEntity> findAllByType(ContentType type);
}