package com.subtrack.backend.ai;

import com.subtrack.backend.ai.dto.AiRecommendationResponse;
import com.subtrack.backend.auth.CurrentUserService;
import com.subtrack.backend.shared.exception.UnauthorizedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles AI recommendation API requests.
 */
@RestController
@RequestMapping("/api/ai")
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;
    private final CurrentUserService currentUserService;

    public AiRecommendationController(
            AiRecommendationService aiRecommendationService,
            CurrentUserService currentUserService
    ) {
        this.aiRecommendationService = aiRecommendationService;
        this.currentUserService = currentUserService;
    }

    /**
     * Returns AI-based subscription recommendations for the authenticated user.
     */
    @GetMapping("/recommendations")
    public AiRecommendationResponse getRecommendations() {
        Long currentUserId = getAuthenticatedUserId();

        return aiRecommendationService.generateRecommendations(currentUserId);
    }

    /**
     * Reads the authenticated user id from JWT context.
     */
    private Long getAuthenticatedUserId() {
        // User identity must come from JWT context, never from frontend request data.
        if (!currentUserService.isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required");
        }

        return currentUserService.getUserId();
    }
}