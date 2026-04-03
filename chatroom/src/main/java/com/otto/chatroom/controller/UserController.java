package com.otto.chatroom.controller;

import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.otto.chatroom.model.dto.LoginRequest;
import com.otto.chatroom.model.dto.UserInfoDTO;
import com.otto.chatroom.model.dto.UserRegisterDTO;
import com.otto.chatroom.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/user")
public class UserController {

  @Autowired
  private UserService userService;

  @Value("${app.avatar-dir:uploads/avatars}")
  private String avatarDir;

  @PostMapping("/register")
  public boolean registerUser(@RequestBody UserRegisterDTO dto) {
    return userService.registerUser(dto);
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest dto, HttpServletRequest request) {
    userService.login(dto, request);
    return ResponseEntity.ok(Map.of("message", "login success"));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(Map.of("message", "logout success"));
  }

  @GetMapping("/me")
  public UserInfoDTO getCurrentUser(Authentication authentication) {
    return userService.getUserInfo(authentication.getName());
  }

  @GetMapping("/info")
  public UserInfoDTO getUserInfo(@RequestParam String username) {
    return userService.getUserInfo(username);
  }

  @PutMapping("/changeDisplayname")
  public boolean changeDisplayName(@RequestBody UserInfoDTO dto, Authentication authentication) {
    dto.setUsername(authentication.getName());
    return userService.changeDisplayName(dto);
  }

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadAvatar(
      @RequestParam("file") MultipartFile file,
      Authentication authentication) {
    String avatarUrl = userService.uploadAvatar(authentication.getName(), file);
    return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
  }

  @GetMapping("/debug/avatar-info")
  public ResponseEntity<?> avatarDebugInfo(Authentication authentication) {
    UserInfoDTO userInfo = userService.getUserInfo(authentication.getName());
    String avatarUrl = userInfo.getAvatarUrl();
    boolean fileExists = false;
    if (avatarUrl != null && avatarUrl.startsWith("/avatars/")) {
      String filename = avatarUrl.substring(8);
      java.nio.file.Path filePath = Paths.get(avatarDir).toAbsolutePath().resolve(filename);
      fileExists = Files.exists(filePath);
    }
    return ResponseEntity.ok(Map.of(
        "username", authentication.getName(),
        "avatarUrl", avatarUrl,
        "avatarDir", Paths.get(avatarDir).toAbsolutePath().toString(),
        "fileExists", fileExists
    ));
  }

}
