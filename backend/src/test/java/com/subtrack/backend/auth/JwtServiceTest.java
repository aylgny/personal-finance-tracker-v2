package com.subtrack.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Set a test-only JWT secret because this unit test does not start the Spring context.
        ReflectionTestUtils.setField(
                jwtService,
                "jwtSecret",
                "test-secret-key-must-be-at-least-32-characters-long"
        );

        // Set token expiration to 24 hours for predictable test behavior.
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);
    }

    @Test
    void generateToken_shouldCreateValidToken_withUserIdAndEmailClaims() {
        // Act: generate a real JWT token using the test secret.
        String token = jwtService.generateToken(10L, "user@test.com");

        // Assert: the token should be valid and contain the expected user information.
        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(10L);
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsInvalid() {
        // Arrange: this string is not a valid JWT token.
        String invalidToken = "invalid-token";

        // Act & Assert: invalid tokens should never be accepted.
        assertThat(jwtService.isTokenValid(invalidToken)).isFalse();
    }
}