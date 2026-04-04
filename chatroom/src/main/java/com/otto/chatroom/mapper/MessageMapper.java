package com.otto.chatroom.mapper;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import com.otto.chatroom.model.dto.ChatMessage;
import com.otto.chatroom.model.entity.MessageEntity;
import com.otto.chatroom.model.entity.RoomEntity;
import com.otto.chatroom.model.entity.UserEntity;

@Component
public class MessageMapper {
  
  public MessageEntity toEntity(ChatMessage message, RoomEntity room, UserEntity sender) {
    return MessageEntity.builder()
        .content(message.getContent())
        .room(room)
        .sender(sender)
        .createdAt(message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now())
        .build();
  }

  public ChatMessage toDto(MessageEntity entity) {
    String senderAvatarUrl = entity.getSender().getAvatarPath() != null 
        ? "/avatars/" + entity.getSender().getAvatarPath() 
        : null;
    return ChatMessage.builder()
        .content(entity.getContent())
        .sender(entity.getSender().getUsername())
      .senderDisplayName(entity.getSender().getDisplayName())
        .createdAt(entity.getCreatedAt())
        .senderAvatarUrl(senderAvatarUrl)
        .build();
  }

}
