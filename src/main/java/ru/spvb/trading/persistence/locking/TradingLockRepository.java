package ru.spvb.trading.persistence.locking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface TradingLockRepository extends Repository<TradingLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lock from TradingLock lock where lock.id = 1")
    TradingLock acquire();
}
