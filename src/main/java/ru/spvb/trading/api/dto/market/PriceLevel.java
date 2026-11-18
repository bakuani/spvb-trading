package ru.spvb.trading.api.dto.market;

import java.math.BigDecimal;

public record PriceLevel(BigDecimal price, long quantity) {
}
