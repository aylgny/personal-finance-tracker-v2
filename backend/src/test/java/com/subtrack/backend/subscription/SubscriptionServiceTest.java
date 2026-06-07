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

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void getSubscriptionsForUser_shouldReturnSubscriptionResponses_forGivenUserId() {
        User user = new User("Aylin", "aylin@test.com", "hashed-password");

        Currency currency = new Currency(
                "TRY",
                "₺",
                "Turkish Lira",
                BigDecimal.ONE
        );

        Category category = new Category(
                "Entertainment",
                1
        );

        PaymentMethod paymentMethod = new PaymentMethod(
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
                "Aylin",
                new BigDecimal("229.99"),
                BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 15),
                true,
                true,
                3,
                "https://netflix.com",
                "Demo subscription"
        );

        when(subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(1L))
                .thenReturn(List.of(subscription));

        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsForUser(1L);

        assertThat(responses).hasSize(1);

        SubscriptionResponse response = responses.get(0);

        assertThat(response.name()).isEqualTo("Netflix");
        assertThat(response.provider()).isEqualTo("Aylin");
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
        assertThat(response.categoryName()).isEqualTo("Entertainment");
        assertThat(response.paymentMethodName()).isEqualTo("Credit Card");
        assertThat(response.websiteUrl()).isEqualTo("https://netflix.com");
        assertThat(response.notes()).isEqualTo("Demo subscription");

        verify(subscriptionRepository).findByUserIdOrderByNextPaymentDateAsc(1L);
    }

    @Test
    void getSubscriptionsForUser_shouldReturnEmptyList_whenUserHasNoSubscriptions() {
        when(subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(2L))
                .thenReturn(List.of());

        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsForUser(2L);

        assertThat(responses).isEmpty();

        verify(subscriptionRepository).findByUserIdOrderByNextPaymentDateAsc(2L);
    }

    @Test
    void createSubscription_shouldCreateSubscription_whenReferenceDataExists() {
        Long userId = 2L;

        User user = new User("Aylin", "aylin@test.com", "hashed-password");

        Currency currency = new Currency(
                "TRY",
                "₺",
                "Turkish Lira",
                BigDecimal.ONE
        );

        Category category = new Category(
                "Entertainment",
                1
        );

        PaymentMethod paymentMethod = new PaymentMethod(
                "Credit Card",
                true,
                1
        );

        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "YouTube Premium",
                "Aylin",
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
                "youtube.com",
                "Personal subscription"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currencyRepository.findById(30L)).thenReturn(Optional.of(currency));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(paymentMethodRepository.findById(20L)).thenReturn(Optional.of(paymentMethod));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponse response = subscriptionService.createSubscription(userId, request);

        assertThat(response.name()).isEqualTo("YouTube Premium");
        assertThat(response.provider()).isEqualTo("Aylin");
        assertThat(response.price()).isEqualByComparingTo("79.99");
        assertThat(response.currencyCode()).isEqualTo("TRY");
        assertThat(response.currencySymbol()).isEqualTo("₺");
        assertThat(response.billingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.categoryName()).isEqualTo("Entertainment");
        assertThat(response.paymentMethodName()).isEqualTo("Credit Card");
        assertThat(response.websiteUrl()).isEqualTo("https://youtube.com");
        assertThat(response.notes()).isEqualTo("Personal subscription");

        verify(currencyRepository).findById(30L);
        verify(categoryRepository).findById(10L);
        verify(paymentMethodRepository).findById(20L);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void createSubscription_shouldCreateSubscription_whenOptionalCategoryAndPaymentMethodAreMissing() {
        Long userId = 2L;

        User user = new User("Aylin", "aylin@test.com", "hashed-password");

        Currency currency = new Currency(
                "USD",
                "$",
                "US Dollar",
                BigDecimal.ONE
        );

        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "Google One",
                "Aylin",
                new BigDecimal("19.99"),
                BillingCycle.YEARLY,
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2027, 6, 7),
                null,
                null,
                null,
                null,
                null,
                30L,
                null,
                "Cloud storage subscription"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currencyRepository.findById(30L)).thenReturn(Optional.of(currency));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponse response = subscriptionService.createSubscription(userId, request);

        assertThat(response.name()).isEqualTo("Google One");
        assertThat(response.currencyCode()).isEqualTo("USD");
        assertThat(response.currencySymbol()).isEqualTo("$");
        assertThat(response.categoryName()).isNull();
        assertThat(response.paymentMethodName()).isNull();
        assertThat(response.websiteUrl()).isNull();
        assertThat(response.autoRenew()).isTrue();
        assertThat(response.notifyEnabled()).isTrue();
        assertThat(response.notifyDaysBefore()).isEqualTo(3);

        verify(currencyRepository).findById(30L);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void createSubscription_shouldThrowException_whenCurrencyDoesNotExist() {
        Long userId = 2L;

        User user = new User("Aylin", "aylin@test.com", "hashed-password");

        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "YouTube Premium",
                "Aylin",
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
                "youtube.com",
                "Personal subscription"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currencyRepository.findById(30L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.createSubscription(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Currency not found");

        verify(currencyRepository).findById(30L);
    }

    @Test
    void updateSubscription_shouldUpdateSubscription_whenSubscriptionBelongsToCurrentUser() {
        Long userId = 2L;
        Long subscriptionId = 5L;

        User user = new User("Aylin", "aylin@test.com", "hashed-password");

        Currency oldCurrency = new Currency(
                "TRY",
                "₺",
                "Turkish Lira",
                BigDecimal.ONE
        );

        Currency newCurrency = new Currency(
                "USD",
                "$",
                "US Dollar",
                BigDecimal.ONE
        );

        Category oldCategory = new Category(
                "Entertainment",
                1
        );

        Category newCategory = new Category(
                "Productivity",
                2
        );

        PaymentMethod oldPaymentMethod = new PaymentMethod(
                "Credit Card",
                true,
                1
        );

        PaymentMethod newPaymentMethod = new PaymentMethod(
                "Debit Card",
                true,
                2
        );

        Subscription subscription = new Subscription(
                user,
                oldCategory,
                oldPaymentMethod,
                oldCurrency,
                "Netflix",
                "Aylin",
                new BigDecimal("229.99"),
                BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 15),
                true,
                true,
                3,
                "https://netflix.com",
                "Old notes"
        );

        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
                "Claude",
                "Aylin",
                new BigDecimal("17.00"),
                BillingCycle.MONTHLY,
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 7, 7),
                true,
                true,
                5,
                11L,
                22L,
                33L,
                "claude.ai",
                "Updated subscription"
        );

        when(subscriptionRepository.findByIdAndUserId(subscriptionId, userId))
                .thenReturn(Optional.of(subscription));
        when(currencyRepository.findById(33L)).thenReturn(Optional.of(newCurrency));
        when(categoryRepository.findById(11L)).thenReturn(Optional.of(newCategory));
        when(paymentMethodRepository.findById(22L)).thenReturn(Optional.of(newPaymentMethod));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponse response = subscriptionService.updateSubscription(userId, subscriptionId, request);

        assertThat(response.name()).isEqualTo("Claude");
        assertThat(response.provider()).isEqualTo("Aylin");
        assertThat(response.price()).isEqualByComparingTo("17.00");
        assertThat(response.currencyCode()).isEqualTo("USD");
        assertThat(response.currencySymbol()).isEqualTo("$");
        assertThat(response.categoryName()).isEqualTo("Productivity");
        assertThat(response.paymentMethodName()).isEqualTo("Debit Card");
        assertThat(response.websiteUrl()).isEqualTo("https://claude.ai");
        assertThat(response.notes()).isEqualTo("Updated subscription");
        assertThat(response.notifyDaysBefore()).isEqualTo(5);

        verify(subscriptionRepository).findByIdAndUserId(subscriptionId, userId);
        verify(currencyRepository).findById(33L);
        verify(categoryRepository).findById(11L);
        verify(paymentMethodRepository).findById(22L);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void updateSubscription_shouldThrowException_whenSubscriptionDoesNotBelongToCurrentUser() {
        Long userId = 2L;
        Long subscriptionId = 999L;

        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
                "Claude",
                "Aylin",
                new BigDecimal("17.00"),
                BillingCycle.MONTHLY,
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 7, 7),
                true,
                true,
                5,
                null,
                null,
                33L,
                "claude.ai",
                "Updated subscription"
        );

        when(subscriptionRepository.findByIdAndUserId(subscriptionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.updateSubscription(userId, subscriptionId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Subscription not found");

        verify(subscriptionRepository).findByIdAndUserId(subscriptionId, userId);
    }

    @Test
    void deleteSubscription_shouldDeleteSubscription_whenSubscriptionBelongsToCurrentUser() {
        Long userId = 2L;
        Long subscriptionId = 5L;

        User user = new User("Aylin", "aylin@test.com", "hashed-password");

        Currency currency = new Currency(
                "TRY",
                "₺",
                "Turkish Lira",
                BigDecimal.ONE
        );

        Subscription subscription = new Subscription(
                user,
                null,
                null,
                currency,
                "Spotify",
                "Aylin",
                new BigDecimal("70.00"),
                BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE,
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 7, 7),
                true,
                true,
                3,
                "https://spotify.com",
                "Music subscription"
        );

        when(subscriptionRepository.findByIdAndUserId(subscriptionId, userId))
                .thenReturn(Optional.of(subscription));

        subscriptionService.deleteSubscription(userId, subscriptionId);

        verify(subscriptionRepository).findByIdAndUserId(subscriptionId, userId);
        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    void deleteSubscription_shouldThrowException_whenSubscriptionDoesNotBelongToCurrentUser() {
        Long userId = 2L;
        Long subscriptionId = 999L;

        when(subscriptionRepository.findByIdAndUserId(subscriptionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.deleteSubscription(userId, subscriptionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Subscription not found");

        verify(subscriptionRepository).findByIdAndUserId(subscriptionId, userId);
    }
}