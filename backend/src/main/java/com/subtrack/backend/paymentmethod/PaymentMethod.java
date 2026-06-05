package com.subtrack.backend.paymentmethod;

import com.subtrack.backend.shared.BaseEntity;
import com.subtrack.backend.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    protected PaymentMethod() {
    }

    public PaymentMethod(User user, String name, Boolean enabled, Integer sortOrder) {
        this.user = user;
        this.name = name;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}