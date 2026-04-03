package com.otto.chatroom.service.Impl;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.otto.chatroom.model.dto.RoomInfoDTO;
import com.otto.chatroom.model.dto.RoomMemberDTO;
import com.otto.chatroom.model.entity.RoomEntity;
import com.otto.chatroom.model.entity.RoomMemberEntity;
import com.otto.chatroom.model.entity.UserEntity;
import com.otto.chatroom.repository.RoomMemberRepository;
import com.otto.chatroom.repository.RoomRepository;
import com.otto.chatroom.repository.UserRepository;
import com.otto.chatroom.service.RoomService;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoomMemberRepository membershipRepository;

  @Override
  public boolean createRoom(String roomName, String username) {
    if (roomName == null || roomName.trim().isEmpty()) {
      throw new IllegalArgumentException("Room name cannot be empty");
    }
    UserEntity userEntity = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    RoomEntity room = RoomEntity.builder()
        .name(roomName)
        .createdBy(userEntity)
        .createdAt(LocalDate.now())
        .build();
    roomRepository.save(room);
    return true;
  }

  @Override
  public boolean deleteRoom(Long roomId) {
    if (!roomRepository.existsById(roomId)) {
      return false;
    }
    roomRepository.deleteById(roomId);
    return true;
  }

  @Override
  public RoomInfoDTO getRoomInfo(Long roomId) {
    RoomEntity room = roomRepository.findById(roomId)
        .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    List<RoomMemberDTO> members = membershipRepository.findByRoomId(roomId).stream()
        .map(m -> {
          String avatarUrl = m.getUser().getAvatarPath() != null ? "/avatars/" + m.getUser().getAvatarPath() : null;
          return RoomMemberDTO.builder()
              .username(m.getUser().getUsername())
              .displayName(m.getUser().getDisplayName())
              .role(m.getRole())
              .avatarUrl(avatarUrl)
              .build();
        })
        .collect(Collectors.toList());
    return RoomInfoDTO.builder()
        .id(room.getId())
        .name(room.getName())
        .members(members)
        .build();
  }

  @Transactional
  @Override
  public boolean joinRoom(Long roomId, String username) {
    if (!roomRepository.existsById(roomId)) {
      throw new IllegalArgumentException("Room not found");
    }
    UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (membershipRepository.findByRoomIdAndUserId(roomId, user.getId()).isPresent()) {
      return false;
    }
    RoomEntity room = roomRepository.findById(roomId).orElseThrow();
    membershipRepository.save(
        RoomMemberEntity.builder()
            .room(room)
            .user(user)
            .role("MEMBER")
            .build()
    );
    return true;
  }

  @Transactional
  @Override
  public boolean leaveRoom(Long roomId, String username) {
    UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (!membershipRepository.findByRoomIdAndUserId(roomId, user.getId()).isPresent()) {
      return false;
    }
    membershipRepository.deleteByRoomIdAndUserId(roomId, user.getId());
    return true;
  }

  @Override
  public List<RoomInfoDTO> listRooms() {
    return roomRepository.findAll().stream()
        .map(room -> RoomInfoDTO.builder()
            .id(room.getId())
            .name(room.getName())
            .members(membershipRepository.findByRoomId(room.getId()).stream()
                .map(m -> RoomMemberDTO.builder()
                    .username(m.getUser().getUsername())
                    .displayName(m.getUser().getDisplayName())
                    .role(m.getRole())
                    .build())
                .collect(Collectors.toList()))
            .build())
        .collect(Collectors.toList());
    }

}
