package ru.spvb.trading.api.dto.trade;

import ru.spvb.trading.domain.trade.Trade;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(long id, String seccode, long buyOrderId, long sellOrderId,
                            BigDecimal price, long quantity, Instant executedAt) {
    public static TradeResponse from(Trade trade) {
        return new TradeResponse(trade.getId(), trade.getSeccode(), trade.getBuyOrder().getId(),
                trade.getSellOrder().getId(), trade.getPrice(), trade.getQuantity(), trade.getExecutedAt());
    }
}
