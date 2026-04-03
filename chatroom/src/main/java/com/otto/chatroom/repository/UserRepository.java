package com.otto.chatroom.repository;

import com.otto.chatroom.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);

    @Query("SELECT u.id FROM UserEntity u WHERE u.username = :username")
    Optional<Long> findIdByUsername(String username);

    @Query("UPDATE UserEntity u SET u.displayName = :displayName WHERE u.username = :username")
    boolean updateDisplayName(String displayName, String username);

    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.avatarPath = :avatarPath WHERE u.username = :username")
    int updateAvatarPath(String avatarPath, String username);
}
