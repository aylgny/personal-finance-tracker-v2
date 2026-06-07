package com.subtrack.backend.subscription;

import com.subtrack.backend.category.Category;
import com.subtrack.backend.category.CategoryRepository;
import com.subtrack.backend.currency.Currency;
import com.subtrack.backend.currency.CurrencyRepository;
import com.subtrack.backend.paymentmethod.PaymentMethod;
import com.subtrack.backend.paymentmethod.PaymentMethodRepository;
import com.subtrack.backend.shared.exception.ResourceNotFoundException;
import com.subtrack.backend.subscription.dto.CreateSubscriptionRequest;
import com.subtrack.backend.subscription.dto.SubscriptionResponse;
import com.subtrack.backend.subscription.dto.UpdateSubscriptionRequest;
import com.subtrack.backend.user.User;
import com.subtrack.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final CurrencyRepository currencyRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            CurrencyRepository currencyRepository,
            CategoryRepository categoryRepository,
            PaymentMethodRepository paymentMethodRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.currencyRepository = currencyRepository;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public List<SubscriptionResponse> getSubscriptionsForUser(Long userId) {
        // Subscriptions are user-owned, so users must only see their own records.
        return subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SubscriptionResponse createSubscription(Long userId, CreateSubscriptionRequest request) {
        // The userId comes from the JWT context, not from the request body.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Currency currency = getCurrencyOrThrow(request.currencyId());
        Category category = getCategoryOrNull(request.categoryId());
        PaymentMethod paymentMethod = getPaymentMethodOrNull(request.paymentMethodId());

        // Apply backend defaults so the frontend does not need to send every optional field.
        Subscription subscription = new Subscription(
                user,
                category,
                paymentMethod,
                currency,
                request.name(),
                request.provider(),
                request.price(),
                request.billingCycle(),
                SubscriptionStatus.ACTIVE,
                request.startDate(),
                request.nextPaymentDate(),
                request.autoRenew() != null ? request.autoRenew() : true,
                request.notifyEnabled() != null ? request.notifyEnabled() : true,
                request.notifyDaysBefore() != null ? request.notifyDaysBefore() : 3,
                normalizeWebsiteUrl(request.websiteUrl()),
                request.notes()
        );

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        return toResponse(savedSubscription);
    }

    public SubscriptionResponse updateSubscription(
            Long userId,
            Long subscriptionId,
            UpdateSubscriptionRequest request
    ) {
        // IDOR protection:
        // The subscription is searched by both subscription id and current user id.
        // This prevents a user from editing another user's subscription.
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        Currency currency = getCurrencyOrThrow(request.currencyId());
        Category category = getCategoryOrNull(request.categoryId());
        PaymentMethod paymentMethod = getPaymentMethodOrNull(request.paymentMethodId());

        subscription.updateDetails(
                category,
                paymentMethod,
                currency,
                request.name(),
                request.provider(),
                request.price(),
                request.billingCycle(),
                request.startDate(),
                request.nextPaymentDate(),
                request.autoRenew() != null ? request.autoRenew() : true,
                request.notifyEnabled() != null ? request.notifyEnabled() : true,
                request.notifyDaysBefore() != null ? request.notifyDaysBefore() : 3,
                normalizeWebsiteUrl(request.websiteUrl()),
                request.notes()
        );

        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        return toResponse(updatedSubscription);
    }

    public void deleteSubscription(Long userId, Long subscriptionId) {
        // IDOR protection:
        // The delete operation also checks the current user id.
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        subscriptionRepository.delete(subscription);
    }

    private Currency getCurrencyOrThrow(Long currencyId) {
        // Currency is required and comes from global reference data.
        return currencyRepository.findById(currencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found"));
    }

    private Category getCategoryOrNull(Long categoryId) {
        // Category is optional and comes from global reference data.
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private PaymentMethod getPaymentMethodOrNull(Long paymentMethodId) {
        // Payment method is optional and comes from global reference data.
        if (paymentMethodId == null) {
            return null;
        }

        return paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"));
    }

    private String normalizeWebsiteUrl(String websiteUrl) {
        // Keep empty values as null and add https:// when the user enters a bare domain.
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return null;
        }

        String trimmedUrl = websiteUrl.trim();

        if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            return trimmedUrl;
        }

        return "https://" + trimmedUrl;
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        // Convert the database entity into a safe API response DTO.
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
                subscription.getWebsiteUrl(),
                subscription.getNotes()
        );
    }
}