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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    // Mocked repositories so unit tests do not use a real database.
    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    // Real service under test. Mockito injects the mocked repositories into it.
    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void getSubscriptionsForUser_shouldReturnSubscriptionResponses_forGivenUserId() {
        // Arrange: create related domain objects used by the subscription entity.
        User user = new User("Aylin", "aylin@test.com", "hashed-password");

        Currency currency = new Currency(
                user,
                "TRY",
                "₺",
                "Turkish Lira",
                BigDecimal.ONE
        );

        Category category = new Category(
                user,
                "Streaming",
                1
        );

        PaymentMethod paymentMethod = new PaymentMethod(
                user,
                "Credit Card",
                true,
                1
        );

        Subscription subscription = new Subscription(
                user,
                category,
                paymentMethod,
                currency,
                "Netflix",
                "Netflix",
                new BigDecimal("229.99"),
                BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 15),
                true,
                true,
                3,
                "Demo subscription"
        );

        // Arrange: simulate repository returning subscriptions for the requested user.
        when(subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(1L))
                .thenReturn(List.of(subscription));

        // Act: call the service method.
        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsForUser(1L);

        // Assert: verify that the service maps the Subscription entity to the response DTO correctly.
        assertThat(responses).hasSize(1);

        SubscriptionResponse response = responses.get(0);

        assertThat(response.name()).isEqualTo("Netflix");
        assertThat(response.provider()).isEqualTo("Netflix");
        assertThat(response.price()).isEqualByComparingTo("229.99");
        assertThat(response.currencyCode()).isEqualTo("TRY");
        assertThat(response.currencySymbol()).isEqualTo("₺");
        assertThat(response.billingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.nextPaymentDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(response.autoRenew()).isTrue();
        assertThat(response.notifyEnabled()).isTrue();
        assertThat(response.notifyDaysBefore()).isEqualTo(3);
        assertThat(response.categoryName()).isEqualTo("Streaming");
        assertThat(response.paymentMethodName()).isEqualTo("Credit Card");
        assertThat(response.notes()).isEqualTo("Demo subscription");

        // Verify: make sure the repository was called with the expected userId.
        verify(subscriptionRepository).findByUserIdOrderByNextPaymentDateAsc(1L);
    }

    @Test
    void getSubscriptionsForUser_shouldReturnEmptyList_whenUserHasNoSubscriptions() {
        // Arrange: simulate that the user has no subscriptions.
        when(subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(2L))
                .thenReturn(List.of());

        // Act: call the service method.
        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsForUser(2L);

        // Assert: service should return an empty response list.
        assertThat(responses).isEmpty();

        // Verify: make sure the repository was called for the correct user.
        verify(subscriptionRepository).findByUserIdOrderByNextPaymentDateAsc(2L);
    }

    @Test
    void createSubscription_shouldCreateSubscription_whenRelatedResourcesBelongToUser() {
        // Arrange: create the authenticated user and related resources.
        Long userId = 2L;

        User user = new User("Aylin", "aylin@test.com", "hashed-password");

        Currency currency = new Currency(
                user,
                "TRY",
                "₺",
                "Turkish Lira",
                BigDecimal.ONE
        );

        Category category = new Category(
                user,
                "Streaming",
                1
        );

        PaymentMethod paymentMethod = new PaymentMethod(
                user,
                "Credit Card",
                true,
                1
        );

        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "YouTube Premium",
                "Google",
                new BigDecimal("79.99"),
                BillingCycle.MONTHLY,
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 7, 7),
                true,
                true,
                3,
                10L,
                20L,
                30L,
                "Personal subscription"
        );

        // Arrange: simulate ownership checks for the authenticated user.
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currencyRepository.findByIdAndUserId(30L, userId)).thenReturn(Optional.of(currency));
        when(categoryRepository.findByIdAndUserId(10L, userId)).thenReturn(Optional.of(category));
        when(paymentMethodRepository.findByIdAndUserId(20L, userId)).thenReturn(Optional.of(paymentMethod));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: call the create method.
        SubscriptionResponse response = subscriptionService.createSubscription(userId, request);

        // Assert: verify that the response contains the created subscription data.
        assertThat(response.name()).isEqualTo("YouTube Premium");
        assertThat(response.provider()).isEqualTo("Google");
        assertThat(response.price()).isEqualByComparingTo("79.99");
        assertThat(response.currencyCode()).isEqualTo("TRY");
        assertThat(response.currencySymbol()).isEqualTo("₺");
        assertThat(response.billingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.categoryName()).isEqualTo("Streaming");
        assertThat(response.paymentMethodName()).isEqualTo("Credit Card");
        assertThat(response.notes()).isEqualTo("Personal subscription");

        // Verify: ensure related resources were checked with both ID and userId to prevent IDOR.
        verify(currencyRepository).findByIdAndUserId(30L, userId);
        verify(categoryRepository).findByIdAndUserId(10L, userId);
        verify(paymentMethodRepository).findByIdAndUserId(20L, userId);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void createSubscription_shouldThrowException_whenCurrencyDoesNotBelongToUser() {
        // Arrange: currency is required and must belong to the authenticated user.
        Long userId = 2L;

        User user = new User("Aylin", "aylin@test.com", "hashed-password");

        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "YouTube Premium",
                "Google",
                new BigDecimal("79.99"),
                BillingCycle.MONTHLY,
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 7, 7),
                true,
                true,
                3,
                null,
                null,
                30L,
                "Personal subscription"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currencyRepository.findByIdAndUserId(30L, userId)).thenReturn(Optional.empty());

        // Act & Assert: the service should reject a currency that is not visible to this user.
        assertThatThrownBy(() -> subscriptionService.createSubscription(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Currency not found");

        verify(currencyRepository).findByIdAndUserId(30L, userId);
    }
}