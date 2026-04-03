package com.otto.chatroom.service;

import java.util.List;
import com.otto.chatroom.model.dto.RoomInfoDTO;

public interface RoomService {

  public boolean createRoom(String roomName, String username);

  public boolean deleteRoom(Long roomId);

  public RoomInfoDTO getRoomInfo(Long roomId);

  public boolean joinRoom(Long roomId, String username);

  public boolean leaveRoom(Long roomId, String username);

  public List<RoomInfoDTO> listRooms();

  

  

}
