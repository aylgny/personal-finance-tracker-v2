package com.subtrack.backend.subscription;

import com.subtrack.backend.category.Category;
import com.subtrack.backend.currency.Currency;
import com.subtrack.backend.paymentmethod.PaymentMethod;
import com.subtrack.backend.subscription.dto.SubscriptionResponse;
import com.subtrack.backend.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    // Mocked repository so this unit test does not use a real database.
    @Mock
    private SubscriptionRepository subscriptionRepository;

    // Real service under test. Mockito injects the mocked repository into it.
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
}