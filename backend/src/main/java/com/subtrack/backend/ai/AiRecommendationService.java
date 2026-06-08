package com.subtrack.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subtrack.backend.ai.dto.AiRecommendationResponse;
import com.subtrack.backend.ai.dto.AiRecommendationResponse.RecommendationItem;
import com.subtrack.backend.ai.dto.GeminiRequest;
import com.subtrack.backend.ai.dto.GeminiResponse;
import com.subtrack.backend.subscription.Subscription;
import com.subtrack.backend.subscription.SubscriptionRepository;
import com.subtrack.backend.user.User;
import com.subtrack.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Generates AI recommendations for subscription savings.
 *
 * Demo users receive fixed recommendations to avoid consuming Gemini quota.
 * Normal users receive Gemini recommendations when an API key exists.
 * If Gemini is not configured or fails, this service returns fallback recommendations.
 */
@Service
public class AiRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(AiRecommendationService.class);

    private static final String DEMO_EMAIL = "demo@subtrack.com";

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey;
    private final String geminiApiUrl;

    public AiRecommendationService(
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent}") String geminiApiUrl
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.geminiApiKey = geminiApiKey;
        this.geminiApiUrl = geminiApiUrl;
    }

    /**
     * Generates recommendations for the authenticated user.
     */
    public AiRecommendationResponse generateRecommendations(Long userId) {
        // Demo account should not consume Gemini quota.
        // It always receives stable portfolio-friendly recommendations.
        if (isDemoUser(userId)) {
            logger.info("Returning hardcoded AI recommendations for demo user.");
            return new AiRecommendationResponse(getDemoRecommendations());
        }

        List<Subscription> subscriptions =
                subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(userId);

        if (subscriptions.isEmpty()) {
            return new AiRecommendationResponse(getEmptyStateRecommendations());
        }

        // If no API key is configured, return safe fallback recommendations.
        // This keeps local development and CI working without external secrets.
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            logger.warn("Gemini API key is missing. Returning fallback recommendations.");
            return new AiRecommendationResponse(getFallbackRecommendations());
        }

        String prompt = buildPrompt(subscriptions);

        try {
            logger.info("Calling Gemini API.");
            logger.info("Gemini API URL configured: {}", geminiApiUrl);
            logger.info("Gemini API key configured: {}", !geminiApiKey.isBlank());

            GeminiResponse geminiResponse = restClient.post()
                    .uri(geminiApiUrl + "?key={apiKey}", geminiApiKey)
                    .body(GeminiRequest.fromPrompt(prompt))
                    .retrieve()
                    .body(GeminiResponse.class);

            String generatedText = geminiResponse == null
                    ? ""
                    : geminiResponse.firstTextOrEmpty();

            logger.info("Gemini raw generated text: {}", generatedText);

            AiRecommendationResponse parsedResponse = parseStructuredRecommendations(generatedText);

            if (parsedResponse == null
                    || parsedResponse.recommendations() == null
                    || parsedResponse.recommendations().isEmpty()) {
                logger.warn("Gemini response was empty or could not be parsed. Returning fallback recommendations.");
                return new AiRecommendationResponse(getFallbackRecommendations());
            }

            return parsedResponse;
        } catch (RestClientResponseException exception) {
            // This logs HTTP errors from Gemini, such as 400, 401, 403, 404, or quota errors.
            logger.error("Gemini API HTTP error. Status: {}", exception.getStatusCode());
            logger.error("Gemini API error body: {}", exception.getResponseBodyAsString());

            return new AiRecommendationResponse(getFallbackRecommendations());
        } catch (Exception exception) {
            // This logs network/configuration errors.
            logger.error("Unexpected Gemini API error: {}", exception.getMessage(), exception);

            return new AiRecommendationResponse(getFallbackRecommendations());
        }
    }

    /**
     * Checks whether the authenticated user is the public demo account.
     */
    private boolean isDemoUser(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .map(email -> email.equalsIgnoreCase(DEMO_EMAIL))
                .orElse(false);
    }

    /**
     * Builds the prompt sent to Gemini.
     */
    private String buildPrompt(List<Subscription> subscriptions) {
        StringBuilder subscriptionSummary = new StringBuilder();

        for (Subscription subscription : subscriptions) {
            subscriptionSummary
                    .append("- Name: ").append(subscription.getName())
                    .append(", Status: ").append(subscription.getStatus())
                    .append(", Price: ");

            if (subscription.getCurrency() != null) {
                subscriptionSummary
                        .append(subscription.getCurrency().getSymbol())
                        .append(subscription.getPrice())
                        .append(" ")
                        .append(subscription.getCurrency().getCode());
            } else {
                subscriptionSummary.append(subscription.getPrice());
            }

            subscriptionSummary
                    .append(", Billing Cycle: ").append(subscription.getBillingCycle())
                    .append(", Category: ")
                    .append(subscription.getCategory() != null ? subscription.getCategory().getName() : "No category")
                    .append(", Payment Method: ")
                    .append(subscription.getPaymentMethod() != null ? subscription.getPaymentMethod().getName() : "No payment method")
                    .append(", Next Payment Date: ").append(subscription.getNextPaymentDate())
                    .append("\n");
        }

        return """
                You are a helpful personal finance assistant inside a subscription tracking app.

                Analyze the user's subscriptions and return exactly 3 useful saving recommendations.

                Return ONLY valid JSON in this exact structure:
                {
                  "recommendations": [
                    {
                      "title": "Short unique title",
                      "description": "One clear sentence explaining the recommendation",
                      "saving": "Short saving estimate or benefit"
                    }
                  ]
                }

                Rules:
                - Do not use markdown.
                - Do not wrap the response in ```json.
                - Do not add text before or after the JSON.
                - Each title must be different.
                - Keep titles short.
                - Keep descriptions practical and simple.
                - Focus on subscription optimization, duplicate services, renewal awareness, inactive subscriptions, and cost control.
                - Do not include sensitive financial advice.

                User subscriptions:
                %s
                """.formatted(subscriptionSummary.toString());
    }

    /**
     * Parses Gemini JSON response into the structured response used by the frontend.
     */
    private AiRecommendationResponse parseStructuredRecommendations(String generatedText) {
        if (generatedText == null || generatedText.isBlank()) {
            return null;
        }

        try {
            String cleanedText = generatedText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(cleanedText, AiRecommendationResponse.class);
        } catch (Exception exception) {
            logger.warn("Could not parse Gemini response as structured JSON: {}", exception.getMessage());
            return null;
        }
    }

    /**
     * Returns fixed recommendations for the public demo account.
     *
     * These recommendations match the seeded demo subscriptions and avoid Gemini quota usage.
     */
    private List<RecommendationItem> getDemoRecommendations() {
        return List.of(
                new RecommendationItem(
                        "Consolidate music apps",
                        "Spotify and YouTube Premium both cover music streaming. Consider keeping one if you do not need both.",
                        "Potential saving: ₺80 monthly"
                ),
                new RecommendationItem(
                        "Review inactive ChatGPT",
                        "ChatGPT is inactive in the demo account. Keeping inactive subscriptions disabled helps prevent unnecessary renewals.",
                        "Avoid ₺1000 monthly charge"
                ),
                new RecommendationItem(
                        "Check high-cost AI tools",
                        "Claude AI is one of the highest monthly costs in this demo. Review whether it is still worth the recurring price.",
                        "Potential saving: ₺750 monthly"
                )
        );
    }

    /**
     * Returns fallback recommendations when Gemini is not available.
     */
    private List<RecommendationItem> getFallbackRecommendations() {
        return List.of(
                new RecommendationItem(
                        "Review inactive subscriptions",
                        "Review inactive subscriptions regularly and keep only the services you still use.",
                        "Potential saving varies"
                ),
                new RecommendationItem(
                        "Compare billing plans",
                        "Compare monthly and yearly billing to find cheaper long-term plans.",
                        "Potential saving: 10-20%"
                ),
                new RecommendationItem(
                        "Check upcoming renewals",
                        "Check upcoming renewals before payment dates to avoid unexpected charges.",
                        "Avoid surprise payments"
                )
        );
    }

    /**
     * Returns recommendations when the user has not added any subscriptions yet.
     */
    private List<RecommendationItem> getEmptyStateRecommendations() {
        return List.of(
                new RecommendationItem(
                        "Add your first subscription",
                        "Add your subscriptions first so AI can analyze your spending.",
                        "No saving estimate yet"
                ),
                new RecommendationItem(
                        "Track recurring services",
                        "Start by tracking recurring services such as streaming, cloud storage, software tools, and memberships.",
                        "Better spending visibility"
                ),
                new RecommendationItem(
                        "Use categories consistently",
                        "Use categories and payment methods consistently to make future insights more accurate.",
                        "Improved recommendations"
                )
        );
    }
}