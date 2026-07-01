package com.mopl.domain.auth.adapter.out;

import com.mopl.domain.auth.domain.PasswordResetToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface PasswordResetTokenEntityMapper {

    PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity);
    PasswordResetTokenJpaEntity toJpaEntity(PasswordResetToken domain);
}
