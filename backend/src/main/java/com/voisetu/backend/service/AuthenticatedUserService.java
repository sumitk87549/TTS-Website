package com.voisetu.backend.service;

import com.voisetu.backend.repository.AppUserRepository;
import com.voisetu.backend.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Resolves the authenticated user's database ID from the JWT principal.
 *
 * Centralizes the repeated pattern of:
 *   userRepository.findByEmail(auth.getName()).orElseThrow().id()
 */
@Service
public class AuthenticatedUserService {

    private final AppUserRepository userRepository;

    public AuthenticatedUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the database ID of the currently authenticated user.
     *
     * @throws ResourceNotFoundException if the user no longer exists in DB (e.g. deleted account)
     */
    public Long userId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", auth.getName()))
                .id();
    }
}
