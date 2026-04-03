package com.otto.chatroom.service;

import com.otto.chatroom.model.dto.ChatMessage;
import java.security.Principal;
import java.util.List;

public interface MessageService {
  void processAndSend(ChatMessage message, Principal principal);

  List<ChatMessage> getMessageHistory(Long roomId);
}
