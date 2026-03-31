package com.otto.chatroom.controller;

import com.otto.chatroom.model.dto.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Controller
public class ChatController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message) {
        // TODO: 權限驗證、DB 寫入等
        Long roomId = message.getRoomId();
        String dest = "/topic/rooms/" + (roomId != null ? roomId : "default");
        messagingTemplate.convertAndSend(dest, message);
    }
}
