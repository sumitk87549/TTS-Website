package com.voisetu.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final AppUserRepository userRepository;
    private final DashboardRepository dashboardRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(AppUserRepository userRepository, DashboardRepository dashboardRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.dashboardRepository = dashboardRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private AppUser getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping
    public Map<String, Object> getMe(Authentication auth) {
        AppUser user = getCurrentUser(auth);
        return Map.of(
            "email", user.email(),
            "displayName", user.displayName(),
            "createdAt", user.createdAt(),
            "isAdmin", user.isAdmin()
        );
    }

    @PatchMapping
    public ResponseEntity<?> updateMe(Authentication auth, @RequestBody Map<String, String> body) {
        AppUser user = getCurrentUser(auth);
        if (body.containsKey("displayName")) {
            userRepository.updateDisplayName(user.id(), body.get("displayName"));
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody Map<String, String> body) {
        AppUser user = getCurrentUser(auth);
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Incorrect current password"));
        }
        userRepository.updatePassword(user.id(), passwordEncoder.encode(newPassword));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(Authentication auth) {
        AppUser user = getCurrentUser(auth);
        // Delete audio files from disk first
        List<String> audioPaths = dashboardRepository.getAllUserAudioPaths(user.id());
        for (String path : audioPaths) {
            new File(path).delete();
        }
        // Cascade delete in DB
        userRepository.deleteById(user.id());
        return ResponseEntity.ok().build();
    }
}
