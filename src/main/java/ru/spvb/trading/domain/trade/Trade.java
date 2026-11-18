package ru.spvb.trading.domain.trade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import ru.spvb.trading.domain.order.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String seccode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buy_order_id")
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sell_order_id")
    private Order sellOrder;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false, updatable = false)
    private Instant executedAt;

    public Trade(Order buyOrder, Order sellOrder, BigDecimal price, long quantity) {
        this.seccode = buyOrder.getInstrument().getSeccode();
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
        this.price = price;
        this.quantity = quantity;
        this.executedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
