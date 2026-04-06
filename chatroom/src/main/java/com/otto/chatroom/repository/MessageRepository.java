package com.otto.chatroom.repository;

import com.otto.chatroom.model.entity.MessageEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

  // 可以根據 roomId 查詢消息歷史
  List<MessageEntity> findByRoomId(Long roomId);

  long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
