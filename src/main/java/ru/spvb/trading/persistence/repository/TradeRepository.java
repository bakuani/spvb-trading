package ru.spvb.trading.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.spvb.trading.domain.trade.Trade;

public interface TradeRepository extends JpaRepository<Trade, Long>, JpaSpecificationExecutor<Trade> {
}
