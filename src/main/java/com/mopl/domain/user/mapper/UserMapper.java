package com.mopl.domain.user.mapper;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
