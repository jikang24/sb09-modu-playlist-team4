package com.mopl.domain.auth.adapter.out;

import com.mopl.domain.auth.domain.PasswordResetToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface PasswordResetTokenEntityMapper {

    @Mapping(source = "tokenHash", target = "temporaryPassword")
    PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity);

    @Mapping(source = "temporaryPassword", target = "tokenHash")
    PasswordResetTokenJpaEntity toJpaEntity(PasswordResetToken domain);
}
