package com.subtrack.backend.paymentmethod;

import com.subtrack.backend.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    protected PaymentMethod() {
    }

    public PaymentMethod(String name, Boolean enabled, Integer sortOrder) {
        this.name = name;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
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