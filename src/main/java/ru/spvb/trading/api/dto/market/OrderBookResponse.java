package ru.spvb.trading.api.dto.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderBookResponse(String seccode, List<PriceLevel> bids, List<PriceLevel> asks,
                                BigDecimal bestBid, BigDecimal bestAsk, Instant asOf) {
    public static OrderBookResponse of(String seccode, List<PriceLevel> bids, List<PriceLevel> asks) {
        return new OrderBookResponse(seccode, bids, asks, bestPrice(bids), bestPrice(asks), Instant.now());
    }

    private static BigDecimal bestPrice(List<PriceLevel> levels) {
        return levels.isEmpty() ? null : levels.getFirst().price();
    }
}
