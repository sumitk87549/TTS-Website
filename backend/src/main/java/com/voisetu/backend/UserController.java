package com.voisetu.backend;

import com.voisetu.backend.dto.request.ChangePasswordRequest;
import com.voisetu.backend.dto.request.UpdateProfileRequest;
import com.voisetu.backend.dto.response.UserProfileResponse;
import com.voisetu.backend.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

/**
 * Current-user profile endpoints.
 *
 * GET    /api/me                  — get profile
 * PATCH  /api/me                  — update display name
 * POST   /api/me/change-password  — change password
 * DELETE /api/me                  — delete account
 */
@RestController
@RequestMapping("/api/me")
public class UserController {

    private final AppUserRepository userRepository;
    private final DashboardRepository dashboardRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(AppUserRepository userRepository,
                          DashboardRepository dashboardRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.dashboardRepository = dashboardRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private AppUser currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping
    public UserProfileResponse getMe(Authentication auth) {
        AppUser user = currentUser(auth);
        return new UserProfileResponse(user.email(), user.displayName(), user.createdAt(), user.isAdmin());
    }

    @PatchMapping
    public ResponseEntity<Void> updateMe(Authentication auth,
                                          @Valid @RequestBody UpdateProfileRequest request) {
        if (request.displayName() != null && !request.displayName().isBlank()) {
            userRepository.updateDisplayName(currentUser(auth).id(), request.displayName().trim());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(Authentication auth,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        AppUser user = currentUser(auth);

        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new ValidationException("WRONG_CURRENT_PASSWORD",
                    "The current password you entered is incorrect.");
        }

        userRepository.updatePassword(user.id(), passwordEncoder.encode(request.newPassword()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(Authentication auth) {
        AppUser user = currentUser(auth);
        List<String> audioPaths = dashboardRepository.getAllUserAudioPaths(user.id());
        audioPaths.forEach(path -> new File(path).delete());
        userRepository.deleteById(user.id());
        return ResponseEntity.noContent().build();
    }
}
