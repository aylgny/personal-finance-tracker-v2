package com.subtrack.backend.subscription;

import com.subtrack.backend.subscription.dto.SubscriptionResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public List<SubscriptionResponse> getSubscriptionsForUser(Long userId) {
        return subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getName(),
                subscription.getProvider(),
                subscription.getPrice(),
                subscription.getCurrency().getCode(),
                subscription.getCurrency().getSymbol(),
                subscription.getBillingCycle(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getNextPaymentDate(),
                subscription.getAutoRenew(),
                subscription.getNotifyEnabled(),
                subscription.getNotifyDaysBefore(),
                subscription.getCategory() != null ? subscription.getCategory().getName() : null,
                subscription.getPaymentMethod() != null ? subscription.getPaymentMethod().getName() : null,
                subscription.getNotes()
        );
    }
}