package com.otto.chatroom.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomInfoDTO {
  private Long id;
  private String name;
  private List<RoomMemberDTO> members;
}
