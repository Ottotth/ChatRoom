package com.otto.chatroom.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    private Long roomId;
    private String content;
    private String type;
    private String sender;
    private String senderDisplayName;
    private LocalDateTime createdAt;
    private String senderAvatarUrl;

}
