package com.otto.chatroom.service.Impl;

import com.otto.chatroom.mapper.MessageMapper;
import com.otto.chatroom.model.dto.ChatMessage;
import com.otto.chatroom.model.entity.MessageEntity;
import com.otto.chatroom.model.entity.RoomEntity;
import com.otto.chatroom.model.entity.UserEntity;
import com.otto.chatroom.repository.MessageRepository;
import com.otto.chatroom.repository.RoomRepository;
import com.otto.chatroom.repository.UserRepository;
import com.otto.chatroom.service.MessageService;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl implements MessageService {

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @Autowired
  private MessageMapper messageMapper;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private MessageRepository messageRepository;

  @Transactional
  @Override
  public void processAndSend(ChatMessage message, Principal principal) {
    Long roomId = message.getRoomId();
    // Save the message to the database
    // 這裡可以根據實際需求決定是否要保存消息
    RoomEntity room = roomRepository.getReferenceById(roomId);
    Long senderId = userRepository.findIdByUsername(principal.getName())
        .orElseThrow(() -> new RuntimeException("User not found"));
    UserEntity sender = userRepository.getReferenceById(senderId);
    MessageEntity messageEntity = messageMapper.toEntity(message, room, sender);
    messageRepository.save(messageEntity);
    // sender 由後端從登入 session 決定，不依賴前端傳入
    message.setSender(principal.getName());
    String senderAvatarUrl = sender.getAvatarPath() != null ? "/avatars/" + sender.getAvatarPath() : null;
    message.setSenderAvatarUrl(senderAvatarUrl);
    String dest = "/topic/rooms/" + (roomId != null ? roomId : "default");
    messagingTemplate.convertAndSend(dest, message);
  }

  @Override
  public List<ChatMessage> getMessageHistory(Long roomId) {
    List<MessageEntity> messages = messageRepository.findByRoomId(roomId);
    return messages.stream()
        .map(messageMapper::toDto)
        .toList();
  }
}
