package com.otto.chatroom.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.otto.chatroom.mapper.UserMapper;
import com.otto.chatroom.model.dto.UserInfoDTO;
import com.otto.chatroom.model.dto.LoginRequest;
import com.otto.chatroom.model.dto.UserRegisterDTO;
import com.otto.chatroom.model.entity.UserEntity;
import com.otto.chatroom.repository.UserRepository;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private UserMapper userMapper;

  public boolean registerUser(UserRegisterDTO dto) {
    if (isUsernameExist(dto.getUsername())) {
    return false;
    }
    UserEntity user = userMapper.toEntity(dto);
    userRepository.save(user);
    return true;
  }

  public UserInfoDTO getUserInfo(String username) {
    UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));
    return UserInfoDTO.builder()
        .username(user.getUsername())
        .displayName(user.getDisplayName())
        .createdAt(user.getCreatedAt())
        .build();
  }

  public boolean validLogin(LoginRequest dto) {
    if (!isUsernameExist(dto.getUsername())) {
      return false;
    }
    UserEntity user = userRepository.findByUsername(dto.getUsername()).get();
    return user.getPassword().equals(dto.getPassword());
  }

  public boolean changeDisplayName(UserInfoDTO dto) {
    return userRepository.updateDisplayName(dto.getDisplayName(), dto.getUsername());
  }

  private boolean isUsernameExist(String username) {
    return userRepository.findByUsername(username).isPresent();
  }
  
  
}
