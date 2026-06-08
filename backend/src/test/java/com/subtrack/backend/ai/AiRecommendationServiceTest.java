package com.subtrack.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subtrack.backend.ai.dto.AiRecommendationResponse;
import com.subtrack.backend.subscription.Subscription;
import com.subtrack.backend.subscription.SubscriptionRepository;
import com.subtrack.backend.user.User;
import com.subtrack.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AiRecommendationService.
 */
class AiRecommendationServiceTest {

    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RestClient.Builder restClientBuilder = mock(RestClient.Builder.class);
    private final RestClient restClient = mock(RestClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Demo user should receive hardcoded recommendations without checking subscriptions
     * or calling Gemini. This prevents demo usage from consuming Gemini quota.
     */
    @Test
    void generateRecommendations_whenDemoUser_returnsHardcodedRecommendations() {
        when(restClientBuilder.build()).thenReturn(restClient);

        AiRecommendationService service = new AiRecommendationService(
                subscriptionRepository,
                userRepository,
                restClientBuilder,
                objectMapper,
                "fake-api-key",
                "https://example.com/gemini"
        );

        User demoUser = new User("Demo User", "demo@subtrack.com", "hashed-password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(demoUser));

        AiRecommendationResponse response = service.generateRecommendations(1L);

        assertEquals(3, response.recommendations().size());
        assertEquals("Consolidate music apps", response.recommendations().get(0).title());
        assertEquals("Review inactive ChatGPT", response.recommendations().get(1).title());
        assertEquals("Check high-cost AI tools", response.recommendations().get(2).title());

        verify(subscriptionRepository, never()).findByUserIdOrderByNextPaymentDateAsc(1L);
    }

    /**
     * Non-demo users with no subscriptions should receive empty-state recommendations.
     */
    @Test
    void generateRecommendations_whenUserHasNoSubscriptions_returnsEmptyStateRecommendations() {
        when(restClientBuilder.build()).thenReturn(restClient);

        AiRecommendationService service = new AiRecommendationService(
                subscriptionRepository,
                userRepository,
                restClientBuilder,
                objectMapper,
                "fake-api-key",
                "https://example.com/gemini"
        );

        User regularUser = new User("Aylin", "aylin@test.com", "hashed-password");

        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(2L)).thenReturn(List.of());

        AiRecommendationResponse response = service.generateRecommendations(2L);

        assertEquals(3, response.recommendations().size());
        assertEquals("Add your first subscription", response.recommendations().get(0).title());
        assertEquals("Track recurring services", response.recommendations().get(1).title());
        assertEquals("Use categories consistently", response.recommendations().get(2).title());
    }

    /**
     * If Gemini API key is missing, the service should return fallback recommendations
     * instead of failing or calling an external API.
     */
    @Test
    void generateRecommendations_whenGeminiApiKeyIsMissing_returnsFallbackRecommendations() {
        when(restClientBuilder.build()).thenReturn(restClient);

        AiRecommendationService service = new AiRecommendationService(
                subscriptionRepository,
                userRepository,
                restClientBuilder,
                objectMapper,
                "",
                "https://example.com/gemini"
        );

        User regularUser = new User("Aylin", "aylin@test.com", "hashed-password");
        Subscription subscription = mock(Subscription.class);

        when(userRepository.findById(3L)).thenReturn(Optional.of(regularUser));
        when(subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(3L))
                .thenReturn(List.of(subscription));

        AiRecommendationResponse response = service.generateRecommendations(3L);

        assertEquals(3, response.recommendations().size());
        assertEquals("Review inactive subscriptions", response.recommendations().get(0).title());
        assertEquals("Compare billing plans", response.recommendations().get(1).title());
        assertEquals("Check upcoming renewals", response.recommendations().get(2).title());

        assertFalse(response.recommendations().get(0).description().isBlank());
        assertFalse(response.recommendations().get(0).saving().isBlank());
    }
}