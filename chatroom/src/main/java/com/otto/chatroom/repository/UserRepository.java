package com.otto.chatroom.repository;

import com.otto.chatroom.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);

    @Query("UPDATE UserEntity u SET u.displayName = :displayName WHERE u.username = :username")
    boolean updateDisplayName(String displayName, String username);
}
