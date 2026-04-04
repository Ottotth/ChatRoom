package com.otto.chatroom.service.Impl;

import com.otto.chatroom.mapper.UserMapper;
import com.otto.chatroom.model.dto.LoginRequest;
import com.otto.chatroom.model.dto.UserInfoDTO;
import com.otto.chatroom.model.dto.UserRegisterDTO;
import com.otto.chatroom.model.entity.UserEntity;
import com.otto.chatroom.repository.UserRepository;
import com.otto.chatroom.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private UserMapper userMapper;

  @Value("${app.avatar-dir:uploads/avatars}")
  private String avatarDir;

  @Override
  public boolean registerUser(UserRegisterDTO dto) {
    if (isUsernameExist(dto.getUsername())) {
      return false;
    }
    UserEntity user = userMapper.toEntity(dto);
    String encodedPassword = passwordEncoder.encode(dto.getPassword());
    user.setPassword(dto.getPassword());
    user.setPasswordHash(encodedPassword);
    userRepository.save(user);
    return true;
  }

  @Override
  public UserInfoDTO getUserInfo(String username) {
    UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));
    String avatarUrl = user.getAvatarPath() != null ? "/avatars/" + user.getAvatarPath() : null;
    return UserInfoDTO.builder()
        .username(user.getUsername())
        .displayName(user.getDisplayName())
        .createdAt(user.getCreatedAt())
        .avatarUrl(avatarUrl)
        .build();
  }

  @Override
  public void login(LoginRequest dto, HttpServletRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
    );
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    HttpSession session = request.getSession(true);
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        context
    );
  }

  @Override
  public boolean changeDisplayName(UserInfoDTO dto) {
    return userRepository.updateDisplayName(dto.getDisplayName(), dto.getUsername()) > 0;
  }

  @Override
  public String uploadAvatar(String username, MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("Only image files are allowed");
    }
    String original = file.getOriginalFilename();
    String ext = "";
    if (original != null) {
      int dotIdx = original.lastIndexOf('.');
      if (dotIdx >= 0) {
        ext = original.substring(dotIdx).toLowerCase();
      }
    }
    if (!List.of(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(ext)) {
      throw new IllegalArgumentException("Unsupported image format");
    }
    String filename = UUID.randomUUID().toString() + ext;
    Path dir = resolveAvatarDir();
    try {
      Files.createDirectories(dir);
      Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save avatar", e);
    }
    userRepository.updateAvatarPath(filename, username);
    return "/avatars/" + filename;
  }

  private boolean isUsernameExist(String username) {
    return userRepository.findByUsername(username).isPresent();
  }

  private Path resolveAvatarDir() {
    Path configured = Paths.get(avatarDir);
    if (configured.isAbsolute()) {
      return configured.normalize();
    }

    Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    Path direct = userDir.resolve(configured).normalize();
    Path chatroomModule = userDir.resolve("chatroom").resolve(configured).normalize();

    if (Files.exists(direct)) {
      return direct;
    }
    if (Files.exists(chatroomModule)) {
      return chatroomModule;
    }

    if (Files.exists(userDir.resolve("pom.xml"))) {
      return direct;
    }
    if (Files.exists(userDir.resolve("chatroom").resolve("pom.xml"))) {
      return chatroomModule;
    }

    return direct;
  }
}
