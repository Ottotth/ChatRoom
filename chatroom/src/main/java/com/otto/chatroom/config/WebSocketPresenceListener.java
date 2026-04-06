package com.otto.chatroom.config;

import com.otto.chatroom.service.RoomService;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class WebSocketPresenceListener {

  private static final Pattern ROOM_TOPIC_PATTERN = Pattern.compile("^/topic/rooms/(\\d+)(?:/.*)?$");

  private final Map<String, Set<Long>> sessionRooms = new ConcurrentHashMap<>();
  private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> roomUserSessions = new ConcurrentHashMap<>();

  @Autowired
  private RoomService roomService;

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();
    String destination = accessor.getDestination();
    Principal principal = accessor.getUser();

    if (sessionId == null || destination == null || principal == null) {
      return;
    }

    Long roomId = extractRoomId(destination);
    if (roomId == null) {
      return;
    }

    String username = principal.getName();
    sessionUsers.putIfAbsent(sessionId, username);

    Set<Long> rooms = sessionRooms.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet());
    boolean firstSubscriptionForRoom = rooms.add(roomId);
    if (!firstSubscriptionForRoom) {
      return;
    }

    String userRoomKey = toUserRoomKey(username, roomId);
    roomUserSessions.computeIfAbsent(userRoomKey, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();
    if (sessionId == null) {
      return;
    }

    String username = sessionUsers.remove(sessionId);
    if (username == null && event.getUser() != null) {
      username = event.getUser().getName();
    }
    if (username == null) {
      return;
    }

    Set<Long> rooms = sessionRooms.remove(sessionId);
    if (rooms == null || rooms.isEmpty()) {
      return;
    }

    for (Long roomId : rooms) {
      String userRoomKey = toUserRoomKey(username, roomId);
      Set<String> sessions = roomUserSessions.get(userRoomKey);
      if (sessions == null) {
        continue;
      }

      sessions.remove(sessionId);
      if (!sessions.isEmpty()) {
        continue;
      }

      roomUserSessions.remove(userRoomKey);

      try {
        boolean left = roomService.leaveRoom(roomId, username);
        if (left) {
          Map<String, Object> eventPayload = Map.of(
              "event", "LEAVE",
              "username", username,
              "roomId", roomId);
          messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/members", (Object) eventPayload);
        }
      } catch (IllegalArgumentException ignored) {
        // User may already be removed or invalid during concurrent disconnect handling.
      }
    }
  }

  private Long extractRoomId(String destination) {
    Matcher matcher = ROOM_TOPIC_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return null;
    }

    try {
      return Long.parseLong(matcher.group(1));
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private String toUserRoomKey(String username, Long roomId) {
    return username + "#" + roomId;
  }
}
