package com.otto.chatroom.service;

import com.otto.chatroom.model.dto.LoginRequest;
import com.otto.chatroom.model.dto.UserInfoDTO;
import com.otto.chatroom.model.dto.UserRegisterDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  boolean registerUser(UserRegisterDTO dto);

  UserInfoDTO getUserInfo(String username);

  void login(LoginRequest dto, HttpServletRequest request);

  boolean changeDisplayName(UserInfoDTO dto);

  String uploadAvatar(String username, MultipartFile file);
}
