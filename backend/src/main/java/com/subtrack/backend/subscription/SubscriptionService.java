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

        // Currency is global reference data, so it is validated by ID only.
        Currency currency = currencyRepository.findById(request.currencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found"));

        // Category is global reference data and optional.
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        // Payment method is global reference data and optional.
        PaymentMethod paymentMethod = null;
        if (request.paymentMethodId() != null) {
            paymentMethod = paymentMethodRepository.findById(request.paymentMethodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"));
        }

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
                request.notes()
        );

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        return toResponse(savedSubscription);
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
                subscription.getNotes()
        );
    }
}