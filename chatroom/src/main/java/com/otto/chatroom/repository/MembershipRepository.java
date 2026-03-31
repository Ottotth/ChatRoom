package com.otto.chatroom.repository;

import com.otto.chatroom.model.entity.MembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<MembershipEntity, Long> {
}
