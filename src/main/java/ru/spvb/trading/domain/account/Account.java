package ru.spvb.trading.domain.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.spvb.trading.domain.TradingException;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    @Id
    private Long id;

    @Column(nullable = false)
    private long userId;

    @Column(nullable = false, precision = 38, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, precision = 38, scale = 4)
    private BigDecimal reserved;

    @Column(nullable = false, length = 3)
    private String currency;

    public BigDecimal available() {
        return balance.subtract(reserved);
    }

    public void reserve(BigDecimal amount) {
        if (available().compareTo(amount) < 0) {
            throw TradingException.conflict("INSUFFICIENT_FUNDS", "Недостаточно свободных денежных средств");
        }
        reserved = reserved.add(amount);
    }

    public void release(BigDecimal amount) {
        reserved = reserved.subtract(amount);
    }

    public void pay(BigDecimal amount, BigDecimal reservation) {
        balance = balance.subtract(amount);
        release(reservation);
    }

    public void receive(BigDecimal amount) {
        balance = balance.add(amount);
    }
}
