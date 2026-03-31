package com.otto.chatroom.mapper;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import com.otto.chatroom.model.dto.UserRegisterDTO;
import com.otto.chatroom.model.entity.UserEntity;

@Component
public class UserMapper {

  public UserEntity toEntity(UserRegisterDTO dto) {
    LocalDate now = LocalDate.now();
    return UserEntity.builder()
        .username(dto.getUsername())
        .displayName(dto.getDisplayName())
        .password(dto.getPassword())
        .createdAt(now)
        .build();
  }
}
