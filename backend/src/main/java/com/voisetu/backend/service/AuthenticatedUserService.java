package com.voisetu.backend.service;

import com.voisetu.backend.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {
    private final AppUserRepository userRepository;

    public AuthenticatedUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long userId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow().id();
    }
}
