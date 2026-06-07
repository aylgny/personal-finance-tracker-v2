package com.subtrack.backend.subscription;

import com.subtrack.backend.auth.CurrentUserService;
import com.subtrack.backend.shared.exception.UnauthorizedException;
import com.subtrack.backend.subscription.dto.SubscriptionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            CurrentUserService currentUserService
    ) {
        this.subscriptionService = subscriptionService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<SubscriptionResponse> getSubscriptions() {
        // Do not trust userId values from the client.
        // The authenticated user ID must come from the JWT-based request context.
        if (!currentUserService.isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required");
        }

        return subscriptionService.getSubscriptionsForUser(currentUserService.getUserId());
    }
}