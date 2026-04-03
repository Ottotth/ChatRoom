package com.otto.chatroom.repository;

import com.otto.chatroom.model.entity.RoomMemberEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMemberEntity, Long> {
  List<RoomMemberEntity> findByRoomId(Long roomId);

  Optional<RoomMemberEntity> findByRoomIdAndUserId(Long roomId, Long userId);

  void deleteByRoomIdAndUserId(Long roomId, Long userId);
}
