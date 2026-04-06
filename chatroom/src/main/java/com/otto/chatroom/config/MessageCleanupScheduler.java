package com.otto.chatroom.config;

import com.otto.chatroom.repository.MessageRepository;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MessageCleanupScheduler {

  private final MessageRepository messageRepository;

  public MessageCleanupScheduler(MessageRepository messageRepository) {
    this.messageRepository = messageRepository;
  }

  @Scheduled(cron = "0 0 */6 * * *")
  @Transactional
  public void deleteMessagesOlderThanSevenDays() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
    messageRepository.deleteByCreatedAtBefore(cutoff);
  }
}
