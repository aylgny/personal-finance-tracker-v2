package com.subtrack.backend.subscription;

import com.subtrack.backend.category.Category;
import com.subtrack.backend.currency.Currency;
import com.subtrack.backend.paymentmethod.PaymentMethod;
import com.subtrack.backend.shared.BaseEntity;
import com.subtrack.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String provider;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 30)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "next_payment_date", nullable = false)
    private LocalDate nextPaymentDate;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew;

    @Column(name = "notify_enabled", nullable = false)
    private Boolean notifyEnabled;

    @Column(name = "notify_days_before", nullable = false)
    private Integer notifyDaysBefore;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(length = 1000)
    private String notes;

    protected Subscription() {
    }

    public Subscription(
            User user,
            Category category,
            PaymentMethod paymentMethod,
            Currency currency,
            String name,
            String provider,
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
        this.user = user;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.currency = currency;
        this.name = name;
        this.provider = provider;
        this.price = price;
        this.billingCycle = billingCycle;
        this.status = status;
        this.startDate = startDate;
        this.nextPaymentDate = nextPaymentDate;
        this.autoRenew = autoRenew;
        this.notifyEnabled = notifyEnabled;
        this.notifyDaysBefore = notifyDaysBefore;
        this.websiteUrl = websiteUrl;
        this.notes = notes;
    }

    public User getUser() {
        return user;
    }

    public Category getCategory() {
        return category;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
    }

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public Boolean getNotifyEnabled() {
        return notifyEnabled;
    }

    public Integer getNotifyDaysBefore() {
        return notifyDaysBefore;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getNotes() {
        return notes;
    }
}