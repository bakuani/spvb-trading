package ru.spvb.trading.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.spvb.trading.domain.TradingException;
import ru.spvb.trading.domain.account.Account;
import ru.spvb.trading.domain.market.Instrument;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity(name = "TradeOrder")
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccode")
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Side type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long filledQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Long replacesOrderId;

    public Order(Account account, Instrument instrument, OrderTerms terms) {
        this.account = account;
        this.instrument = instrument;
        this.type = terms.type();
        this.price = terms.price().setScale(4);
        this.quantity = terms.quantity();
        this.status = OrderStatus.ACTIVE;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        this.updatedAt = createdAt;
    }

    public Order replacement(OrderTerms terms) {
        Order replacement = new Order(account, instrument, terms);
        replacement.replacesOrderId = id;
        return replacement;
    }

    public long remainingQuantity() {
        return quantity - filledQuantity;
    }

    public boolean isActive() {
        return status == OrderStatus.ACTIVE || status == OrderStatus.PARTIALLY_FILLED;
    }

    public void requireActive() {
        if (!isActive()) {
            throw TradingException.conflict("ORDER_NOT_ACTIVE", "Можно изменять только неисполненный остаток активной заявки");
        }
    }

    public void fill(long amount) {
        requireActive();
        if (amount <= 0 || amount > remainingQuantity()) {
            throw new IllegalArgumentException("Fill quantity must be positive and within the remaining quantity");
        }
        filledQuantity += amount;
        status = filledQuantity == quantity ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public void cancel() {
        requireActive();
        status = OrderStatus.CANCELLED;
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
