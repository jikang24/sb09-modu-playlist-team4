package com.mopl.domain.follow.adapter.out.persistence;

import com.mopl.domain.follow.adapter.out.FollowJpaEntity;
import com.mopl.domain.follow.domain.Follow;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FollowPersistenceMapper {
    FollowJpaEntity toJpaEntity(Follow follow);
    Follow toDomain(FollowJpaEntity entity);
}
