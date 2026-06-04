package com.subtrack.backend.currency;

import com.subtrack.backend.shared.BaseEntity;
import com.subtrack.backend.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "currencies")
public class Currency extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 3)
    private String code;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    protected Currency() {
    }

    public Currency(User user, String code, String symbol, String name, BigDecimal exchangeRate) {
        this.user = user;
        this.code = code;
        this.symbol = symbol;
        this.name = name;
        this.exchangeRate = exchangeRate;
    }

    public User getUser() {
        return user;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }
}