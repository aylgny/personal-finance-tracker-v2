package com.subtrack.backend.auth;

import com.subtrack.backend.auth.dto.AuthResponse;
import com.subtrack.backend.auth.dto.LoginRequest;
import com.subtrack.backend.auth.dto.RegisterRequest;
import com.subtrack.backend.shared.exception.DuplicateResourceException;
import com.subtrack.backend.shared.exception.UnauthorizedException;
import com.subtrack.backend.user.User;
import com.subtrack.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        // Prevent duplicate accounts with the same email address.
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        // Store only the hashed password, never the raw password.
        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                request.name(),
                request.email(),
                passwordHash
        );

        // Save the new user before generating the JWT because the generated user ID is needed.
        User savedUser = userRepository.save(user);

        // Reference data such as currencies, categories, and payment methods is global.
        // Therefore, no user-specific setup records are created during registration.
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    public AuthResponse login(LoginRequest request) {
        // Use a generic error message so attackers cannot learn whether the email exists.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Compare the raw password from the request with the stored password hash.
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Generate a JWT after the credentials are verified.
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}