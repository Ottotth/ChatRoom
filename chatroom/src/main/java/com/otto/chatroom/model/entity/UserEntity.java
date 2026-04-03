package com.otto.chatroom.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String displayName;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private LocalDate createdAt;

    @Column(name = "avatar_path")
    private String avatarPath;

}
