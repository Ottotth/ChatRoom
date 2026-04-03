package com.otto.chatroom.model.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMemberDTO {
  private String username;
  private String displayName;
  private String role;
  private LocalDate joinedAt;
  private String avatarUrl;
}
