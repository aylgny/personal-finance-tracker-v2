package com.subtrack.backend.currency;

import com.subtrack.backend.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "currencies")
public class Currency extends BaseEntity {

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    protected Currency() {
    }

    public Currency(String code, String symbol, String name, BigDecimal exchangeRate) {
        this.code = code;
        this.symbol = symbol;
        this.name = name;
        this.exchangeRate = exchangeRate;
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