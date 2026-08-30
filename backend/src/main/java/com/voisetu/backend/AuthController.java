package com.voisetu.backend;

import com.voisetu.backend.dto.request.RegisterRequest;
import com.voisetu.backend.dto.request.LoginRequest;
import com.voisetu.backend.dto.response.AuthResponse;
import com.voisetu.backend.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthController(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ValidationException("EMAIL_ALREADY_EXISTS",
                    "An account with this email address already exists.");
        }

        userRepository.save(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName()
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);
        AppUser user = userRepository.findByEmail(request.email()).orElseThrow();

        return ResponseEntity.ok(new AuthResponse(token, user.id(), user.displayName(), user.isAdmin()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // BadCredentialsException is caught by GlobalExceptionHandler → 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);
        AppUser user = userRepository.findByEmail(request.email()).orElseThrow();

        return ResponseEntity.ok(new AuthResponse(token, user.id(), user.displayName(), user.isAdmin()));
    }
}
