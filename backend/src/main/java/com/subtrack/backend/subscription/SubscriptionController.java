package com.subtrack.backend.subscription;

import com.subtrack.backend.subscription.dto.SubscriptionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private static final Long DEMO_USER_ID = 1L;

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public List<SubscriptionResponse> getSubscriptions() {
        return subscriptionService.getSubscriptionsForUser(DEMO_USER_ID);
    }
}