package com.otto.chatroom.controller;

import com.otto.chatroom.model.dto.ChatMessage;
import com.otto.chatroom.service.MessageService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class ChatController {
    @Autowired
    private MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message, Principal principal) {
        messageService.processAndSend(message, principal);
    }
}

@RestController
@RequestMapping("/messages")
class MessageHistoryController {
    @Autowired
    private MessageService messageService;

    @GetMapping("/rooms/{roomId}")
    public List<ChatMessage> getHistory(@PathVariable Long roomId) {
        return messageService.getMessageHistory(roomId);
    }
}
