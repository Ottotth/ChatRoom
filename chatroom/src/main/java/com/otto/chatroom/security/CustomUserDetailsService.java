package com.otto.chatroom.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.otto.chatroom.model.entity.UserEntity;
import com.otto.chatroom.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String storedPassword = user.getPasswordHash() != null ? user.getPasswordHash() : user.getPassword();

        return User.withUsername(user.getUsername())
            .password(storedPassword)
            .roles("USER")
            .build();
    }
}
