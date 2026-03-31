package com.otto.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.otto.chatroom.model.dto.LoginRequest;
import com.otto.chatroom.model.dto.UserInfoDTO;
import com.otto.chatroom.model.dto.UserRegisterDTO;
import com.otto.chatroom.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;





@RestController
@RequestMapping("/user")
public class UserController {

  @Autowired
  private UserService userService;

  @PostMapping("/register")
  public boolean registerUser(@RequestBody UserRegisterDTO dto) {
      return userService.registerUser(dto);
  }

  @GetMapping("/info")
  public UserInfoDTO getUserInfo(@RequestParam String username) {
      return userService.getUserInfo(username);
  }

  @PutMapping("/changeDisplayname")
  public boolean changeDisplayName(@RequestBody UserInfoDTO dto) {
      return userService.changeDisplayName(dto);
  }

  @PostMapping("/login")
  public boolean validLogin(@RequestBody LoginRequest dto) {
      return userService.validLogin(dto);
  }
  
  
  
  
}
