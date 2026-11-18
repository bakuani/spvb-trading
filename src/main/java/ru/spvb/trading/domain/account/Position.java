package ru.spvb.trading.domain.account;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.spvb.trading.domain.TradingException;

@Entity
@Table(name = "positions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position {
    @EmbeddedId
    private PositionId id;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long reserved;

    public Position(PositionId id) {
        this.id = id;
    }

    public long available() {
        return quantity - reserved;
    }

    public void reserve(long amount) {
        if (available() < amount) {
            throw TradingException.conflict("INSUFFICIENT_SECURITIES", "Недостаточно свободных бумаг");
        }
        reserved += amount;
    }

    public void release(long amount) {
        reserved -= amount;
    }

    public void deliver(long amount) {
        quantity -= amount;
        release(amount);
    }

    public void receive(long amount) {
        quantity += amount;
    }
}
