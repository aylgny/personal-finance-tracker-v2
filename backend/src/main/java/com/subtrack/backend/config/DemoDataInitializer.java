package com.subtrack.backend.config;

import com.subtrack.backend.category.Category;
import com.subtrack.backend.category.CategoryRepository;
import com.subtrack.backend.currency.Currency;
import com.subtrack.backend.currency.CurrencyRepository;
import com.subtrack.backend.paymentmethod.PaymentMethod;
import com.subtrack.backend.paymentmethod.PaymentMethodRepository;
import com.subtrack.backend.subscription.BillingCycle;
import com.subtrack.backend.subscription.Subscription;
import com.subtrack.backend.subscription.SubscriptionRepository;
import com.subtrack.backend.subscription.SubscriptionStatus;
import com.subtrack.backend.user.User;
import com.subtrack.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Seeds a public demo account for portfolio usage.
 *
 * The demo account allows visitors to try the app without registration.
 * Demo subscriptions are synced on startup so portfolio data stays consistent.
 */
@Component
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final String DEMO_NAME = "Demo User";
    private static final String DEMO_EMAIL = "demo@subtrack.com";
    private static final String DEMO_PASSWORD = "demo";

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CurrencyRepository currencyRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            CurrencyRepository currencyRepository,
            CategoryRepository categoryRepository,
            PaymentMethodRepository paymentMethodRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.currencyRepository = currencyRepository;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User demoUser = userRepository.findByEmail(DEMO_EMAIL)
                .map(this::refreshDemoPassword)
                .orElseGet(this::createDemoUser);

        syncDemoSubscriptions(demoUser);
    }

    /**
     * Creates the demo user if it does not exist.
     */
    private User createDemoUser() {
        logger.info("Creating demo user: {}", DEMO_EMAIL);

        User demoUser = new User(
                DEMO_NAME,
                DEMO_EMAIL,
                passwordEncoder.encode(DEMO_PASSWORD)
        );

        return userRepository.save(demoUser);
    }

    /**
     * Refreshes the demo password on every startup.
     *
     * This guarantees that demo login works even if the existing database
     * contains an old, invalid, or plain text password value.
     */
    private User refreshDemoPassword(User demoUser) {
        logger.info("Refreshing demo user password hash: {}", DEMO_EMAIL);

        demoUser.updatePasswordHash(passwordEncoder.encode(DEMO_PASSWORD));

        return userRepository.save(demoUser);
    }

    /**
     * Creates missing demo subscriptions and updates existing ones.
     *
     * This keeps demo data stable for portfolio visitors and also ensures
     * website URLs are always available for favicon logos.
     */
    private void syncDemoSubscriptions(User demoUser) {
        List<Subscription> existingDemoSubscriptions =
                subscriptionRepository.findByUserIdOrderByNextPaymentDateAsc(demoUser.getId());

        Map<String, Subscription> existingSubscriptionsByName = existingDemoSubscriptions
                .stream()
                .collect(Collectors.toMap(
                        subscription -> normalizeName(subscription.getName()),
                        subscription -> subscription,
                        (first, second) -> first
                ));

        List<DemoSubscriptionData> demoSubscriptionData = buildDemoSubscriptionData();

        int createdCount = 0;
        int updatedCount = 0;

        for (DemoSubscriptionData data : demoSubscriptionData) {
            Subscription existingSubscription =
                    existingSubscriptionsByName.get(normalizeName(data.name()));

            if (existingSubscription == null) {
                subscriptionRepository.save(createSubscription(demoUser, data));
                createdCount++;
            } else {
                updateSubscription(existingSubscription, data);
                subscriptionRepository.save(existingSubscription);
                updatedCount++;
            }
        }

        logger.info(
                "Demo subscriptions synced. Created: {}, Updated: {}",
                createdCount,
                updatedCount
        );
    }

    /**
     * Defines the portfolio demo subscription data.
     */
    private List<DemoSubscriptionData> buildDemoSubscriptionData() {
        LocalDate today = LocalDate.now();

        return List.of(
                new DemoSubscriptionData(
                        "Netflix",
                        "Netflix",
                        "Entertainment",
                        "Credit Card",
                        "TRY",
                        new BigDecimal("229.99"),
                        BillingCycle.MONTHLY,
                        SubscriptionStatus.ACTIVE,
                        today.minusMonths(10),
                        today.plusDays(5),
                        true,
                        true,
                        3,
                        "https://www.netflix.com",
                        "Streaming subscription for movies and series."
                ),
                new DemoSubscriptionData(
                        "Spotify",
                        "Spotify",
                        "Music",
                        "Credit Card",
                        "TRY",
                        new BigDecimal("80.00"),
                        BillingCycle.MONTHLY,
                        SubscriptionStatus.ACTIVE,
                        today.minusMonths(8),
                        today.plusDays(13),
                        true,
                        true,
                        3,
                        "https://www.spotify.com",
                        "Music streaming subscription."
                ),
                new DemoSubscriptionData(
                        "Google One",
                        "Google",
                        "Cloud Storage",
                        "Credit Card",
                        "TRY",
                        new BigDecimal("49.99"),
                        BillingCycle.MONTHLY,
                        SubscriptionStatus.ACTIVE,
                        today.minusMonths(7),
                        today.plusDays(18),
                        true,
                        true,
                        5,
                        "https://one.google.com",
                        "Cloud storage subscription."
                ),
                new DemoSubscriptionData(
                        "Trendyol Plus",
                        "Trendyol",
                        "Other",
                        "Debit Card",
                        "TRY",
                        new BigDecimal("79.90"),
                        BillingCycle.MONTHLY,
                        SubscriptionStatus.ACTIVE,
                        today.minusMonths(3),
                        today.plusDays(6),
                        true,
                        true,
                        3,
                        "https://www.trendyol.com",
                        "Shopping and delivery membership."
                ),
                new DemoSubscriptionData(
                        "Claude AI",
                        "Anthropic",
                        "Productivity",
                        "Credit Card",
                        "TRY",
                        new BigDecimal("750.00"),
                        BillingCycle.MONTHLY,
                        SubscriptionStatus.ACTIVE,
                        today.minusMonths(2),
                        today.plusDays(11),
                        true,
                        true,
                        5,
                        "https://claude.ai",
                        "AI assistant subscription."
                ),
                new DemoSubscriptionData(
                        "LinkedIn Premium",
                        "LinkedIn",
                        "Productivity",
                        "Credit Card",
                        "TRY",
                        new BigDecimal("120.00"),
                        BillingCycle.MONTHLY,
                        SubscriptionStatus.ACTIVE,
                        today.minusMonths(6),
                        today.plusDays(13),
                        true,
                        true,
                        5,
                        "https://www.linkedin.com",
                        "Professional networking subscription."
                ),
                new DemoSubscriptionData(
                        "YouTube Premium",
                        "YouTube",
                        "Entertainment",
                        "Credit Card",
                        "TRY",
                        new BigDecimal("79.99"),
                        BillingCycle.MONTHLY,
                        SubscriptionStatus.ACTIVE,
                        today.minusMonths(5),
                        today.plusDays(29),
                        true,
                        true,
                        3,
                        "https://www.youtube.com",
                        "Includes ad-free YouTube and YouTube Music."
                ),
                new DemoSubscriptionData(
                        "ChatGPT",
                        "OpenAI",
                        "Productivity",
                        "Other",
                        "TRY",
                        new BigDecimal("1000.00"),
                        BillingCycle.MONTHLY,
                        SubscriptionStatus.INACTIVE,
                        today.minusMonths(4),
                        today.plusDays(41),
                        false,
                        false,
                        0,
                        "https://chatgpt.com",
                        "Inactive subscription used to demonstrate savings."
                )
        );
    }

    /**
     * Creates a new subscription from demo data.
     */
    private Subscription createSubscription(User demoUser, DemoSubscriptionData data) {
        return new Subscription(
                demoUser,
                findCategoryByName(data.categoryName()),
                findPaymentMethodByName(data.paymentMethodName()),
                findCurrencyByCode(data.currencyCode()),
                data.name(),
                data.provider(),
                data.price(),
                data.billingCycle(),
                data.status(),
                data.startDate(),
                data.nextPaymentDate(),
                data.autoRenew(),
                data.notifyEnabled(),
                data.notifyDaysBefore(),
                data.websiteUrl(),
                data.notes()
        );
    }

    /**
     * Updates an existing demo subscription from demo data.
     */
    private void updateSubscription(Subscription subscription, DemoSubscriptionData data) {
        subscription.updateDetails(
                findCategoryByName(data.categoryName()),
                findPaymentMethodByName(data.paymentMethodName()),
                findCurrencyByCode(data.currencyCode()),
                data.name(),
                data.provider(),
                data.price(),
                data.billingCycle(),
                data.startDate(),
                data.nextPaymentDate(),
                data.autoRenew(),
                data.notifyEnabled(),
                data.notifyDaysBefore(),
                data.websiteUrl(),
                data.notes()
        );

        subscription.changeStatus(data.status());
    }

    /**
     * Normalizes subscription names for duplicate-safe matching.
     */
    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }

        return name.trim().toLowerCase();
    }

    /**
     * Finds a global currency by code.
     */
    private Currency findCurrencyByCode(String code) {
        return currencyRepository.findAllByOrderByCodeAsc()
                .stream()
                .filter(currency -> currency.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Required currency not found: " + code));
    }

    /**
     * Finds a global category by name.
     */
    private Category findCategoryByName(String name) {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .filter(category -> category.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Required category not found: " + name));
    }

    /**
     * Finds an enabled global payment method by name.
     */
    private PaymentMethod findPaymentMethodByName(String name) {
        return paymentMethodRepository.findByEnabledTrueOrderBySortOrderAscNameAsc()
                .stream()
                .filter(paymentMethod -> paymentMethod.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Required payment method not found: " + name));
    }

    /**
     * Demo subscription seed data.
     */
    private record DemoSubscriptionData(
            String name,
            String provider,
            String categoryName,
            String paymentMethodName,
            String currencyCode,
            BigDecimal price,
            BillingCycle billingCycle,
            SubscriptionStatus status,
            LocalDate startDate,
            LocalDate nextPaymentDate,
            Boolean autoRenew,
            Boolean notifyEnabled,
            Integer notifyDaysBefore,
            String websiteUrl,
            String notes
    ) {
    }
}