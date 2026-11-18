package ru.spvb.trading.persistence.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trading_lock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradingLock {
    @Id
    private Long id;
}
