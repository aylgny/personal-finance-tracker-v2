package com.subtrack.backend.subscription;

import com.subtrack.backend.auth.CurrentUserService;
import com.subtrack.backend.shared.exception.UnauthorizedException;
import com.subtrack.backend.subscription.dto.CreateSubscriptionRequest;
import com.subtrack.backend.subscription.dto.SubscriptionResponse;
import com.subtrack.backend.subscription.dto.UpdateSubscriptionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
        Long currentUserId = getAuthenticatedUserId();

        return subscriptionService.getSubscriptionsForUser(currentUserId);
    }

    @PostMapping
    public SubscriptionResponse createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request
    ) {
        Long currentUserId = getAuthenticatedUserId();

        return subscriptionService.createSubscription(currentUserId, request);
    }

    @PutMapping("/{subscriptionId}")
    public SubscriptionResponse updateSubscription(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody UpdateSubscriptionRequest request
    ) {
        Long currentUserId = getAuthenticatedUserId();

        return subscriptionService.updateSubscription(currentUserId, subscriptionId, request);
    }

    @DeleteMapping("/{subscriptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubscription(@PathVariable Long subscriptionId) {
        Long currentUserId = getAuthenticatedUserId();

        subscriptionService.deleteSubscription(currentUserId, subscriptionId);
    }

    private Long getAuthenticatedUserId() {
        // Never trust userId values sent from the frontend.
        // The authenticated user ID must always come from the JWT request context.
        if (!currentUserService.isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required");
        }

        return currentUserService.getUserId();
    }
}