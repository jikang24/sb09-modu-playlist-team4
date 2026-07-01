package com.mopl.domain.dm.adapter.out.persistence;

import com.mopl.domain.dm.domain.DirectMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DirectMessagePersistenceMapper {
  DirectMessageJpaEntity toJpaEntity(DirectMessage directMessage);
  DirectMessage toDomain(DirectMessageJpaEntity directMessageJpaEntity);

}
