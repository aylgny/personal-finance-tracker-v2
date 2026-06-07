package com.subtrack.backend.auth;

import com.subtrack.backend.auth.dto.AuthResponse;
import com.subtrack.backend.auth.dto.LoginRequest;
import com.subtrack.backend.auth.dto.RegisterRequest;
import com.subtrack.backend.user.User;
import com.subtrack.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Mocked repository so the test does not use a real database.
    @Mock
    private UserRepository userRepository;

    // Mocked password encoder so we can control password hashing and matching behavior.
    @Mock
    private PasswordEncoder passwordEncoder;

    // Mocked JWT service so the test does not depend on real JWT generation logic.
    @Mock
    private JwtService jwtService;

    // AuthService is the real class under test.
    // Mockito injects the mocked dependencies above into this service.
    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUserAndReturnToken_whenEmailIsNotUsed() {
        // Arrange: create a register request as if it came from the frontend.
        RegisterRequest request = new RegisterRequest(
                "Aylin",
                "aylin@test.com",
                "123456"
        );

        // Arrange: simulate the user object returned after saving to the database.
        User savedUser = new User(
                "Aylin",
                "aylin@test.com",
                "hashed-password"
        );

        // Arrange: define how mocked dependencies should behave in this scenario.
        when(userRepository.existsByEmail("aylin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser.getId(), savedUser.getEmail()))
                .thenReturn("mock-jwt-token");

        // Act: call the actual register method.
        AuthResponse response = authService.register(request);

        // Assert: verify that the response contains the expected auth data.
        assertThat(response.token()).isEqualTo("mock-jwt-token");
        assertThat(response.name()).isEqualTo("Aylin");
        assertThat(response.email()).isEqualTo("aylin@test.com");

        // Verify: make sure the service followed the expected register flow.
        verify(userRepository).existsByEmail("aylin@test.com");
        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(savedUser.getId(), savedUser.getEmail());
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        // Arrange: create a request using an email that already exists.
        RegisterRequest request = new RegisterRequest(
                "Aylin",
                "aylin@test.com",
                "123456"
        );

        // Arrange: simulate that this email is already used.
        when(userRepository.existsByEmail("aylin@test.com")).thenReturn(true);

        // Act & Assert: registration should fail with a clear exception message.
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already in use");

        // Verify: when email already exists, no password hashing, saving, or token generation should happen.
        verify(userRepository).existsByEmail("aylin@test.com");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        // Arrange: create a login request with valid credentials.
        LoginRequest request = new LoginRequest(
                "aylin@test.com",
                "123456"
        );

        // Arrange: simulate an existing user with a hashed password in the database.
        User user = new User(
                "Aylin",
                "aylin@test.com",
                "hashed-password"
        );

        // Arrange: simulate successful user lookup, password match, and token generation.
        when(userRepository.findByEmail("aylin@test.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hashed-password"))
                .thenReturn(true);
        when(jwtService.generateToken(user.getId(), user.getEmail()))
                .thenReturn("mock-jwt-token");

        // Act: call the actual login method.
        AuthResponse response = authService.login(request);

        // Assert: verify that successful login returns the expected token and user info.
        assertThat(response.token()).isEqualTo("mock-jwt-token");
        assertThat(response.name()).isEqualTo("Aylin");
        assertThat(response.email()).isEqualTo("aylin@test.com");

        // Verify: make sure login checked the user, validated the password, and generated a token.
        verify(userRepository).findByEmail("aylin@test.com");
        verify(passwordEncoder).matches("123456", "hashed-password");
        verify(jwtService).generateToken(user.getId(), user.getEmail());
    }

    @Test
    void login_shouldThrowException_whenEmailDoesNotExist() {
        // Arrange: create a login request for an email that does not exist.
        LoginRequest request = new LoginRequest(
                "missing@test.com",
                "123456"
        );

        // Arrange: simulate that no user was found for this email.
        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        // Act & Assert: login should fail with a generic invalid credentials message.
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email or password");

        // Verify: if the user does not exist, password matching and token generation should not happen.
        verify(userRepository).findByEmail("missing@test.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(), anyString());
    }

    @Test
    void login_shouldThrowException_whenPasswordIsWrong() {
        // Arrange: create a login request with an existing email but wrong password.
        LoginRequest request = new LoginRequest(
                "aylin@test.com",
                "wrong-password"
        );

        // Arrange: simulate an existing user.
        User user = new User(
                "Aylin",
                "aylin@test.com",
                "hashed-password"
        );

        // Arrange: simulate successful user lookup but failed password match.
        when(userRepository.findByEmail("aylin@test.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password"))
                .thenReturn(false);

        // Act & Assert: login should fail when the password does not match.
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email or password");

        // Verify: token generation should not happen when the password is wrong.
        verify(userRepository).findByEmail("aylin@test.com");
        verify(passwordEncoder).matches("wrong-password", "hashed-password");
        verify(jwtService, never()).generateToken(any(), anyString());
    }


}