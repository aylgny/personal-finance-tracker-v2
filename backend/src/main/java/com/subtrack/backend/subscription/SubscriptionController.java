package com.subtrack.backend.subscription;

import com.subtrack.backend.auth.CurrentUserService;
import com.subtrack.backend.subscription.dto.SubscriptionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
        if (!currentUserService.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication is required");
        }

        return subscriptionService.getSubscriptionsForUser(currentUserService.getUserId());
    }
}