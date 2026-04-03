package com.otto.chatroom.controller;

import com.otto.chatroom.model.dto.CreateRoomRequest;
import com.otto.chatroom.model.dto.RoomInfoDTO;
import com.otto.chatroom.service.RoomService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {

  @Autowired
  private RoomService roomService;

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @PostMapping
  public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequest request,
      Authentication authentication) {
    String roomName = request.getName();

    roomService.createRoom(roomName, authentication.getName());
    return ResponseEntity.status(201).body(Map.of("message", "Room created"));
  }

  @GetMapping("/{roomId}")
  public RoomInfoDTO getRoomInfo(@PathVariable Long roomId) {
    return roomService.getRoomInfo(roomId);
  }

  @GetMapping
  public List<RoomInfoDTO> listRooms() {
    return roomService.listRooms();
  }

  @PostMapping("/{roomId}/members/join")
  public ResponseEntity<?> joinRoom(@PathVariable Long roomId, Authentication authentication) {
    roomService.joinRoom(roomId, authentication.getName());
    Map<String, Object> eventPayload = Map.of(
        "event", "JOIN",
        "username", authentication.getName(),
        "roomId", roomId);
    messagingTemplate.convertAndSend(
        "/topic/rooms/" + roomId + "/members",
        (Object) eventPayload);
    return ResponseEntity.ok(Map.of("message", "Joined room"));
  }

  @DeleteMapping("/{roomId}/members/leave")
  public ResponseEntity<?> leaveRoom(@PathVariable Long roomId, Authentication authentication) {
    boolean success = roomService.leaveRoom(roomId, authentication.getName());
    if (!success) {
      return ResponseEntity.badRequest().body(Map.of("message", "Not a member of this room"));
    }
    Map<String, Object> eventPayload = Map.of(
        "event", "LEAVE",
        "username", authentication.getName(),
        "roomId", roomId);
    messagingTemplate.convertAndSend(
        "/topic/rooms/" + roomId + "/members",
        (Object) eventPayload);
    return ResponseEntity.ok(Map.of("message", "Left room"));
  }

  @PostMapping("/{roomId}/members/disconnect")
  public ResponseEntity<?> disconnectRoom(@PathVariable Long roomId, Authentication authentication) {
    boolean success = roomService.leaveRoom(roomId, authentication.getName());
    if (success) {
      Map<String, Object> eventPayload = Map.of(
          "event", "LEAVE",
          "username", authentication.getName(),
          "roomId", roomId);
      messagingTemplate.convertAndSend(
          "/topic/rooms/" + roomId + "/members",
          (Object) eventPayload);
    }
    return ResponseEntity.ok(Map.of("message", success ? "Disconnected and left room" : "Already left room"));
  }
}
