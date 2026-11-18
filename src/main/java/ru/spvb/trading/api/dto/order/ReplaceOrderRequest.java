package ru.spvb.trading.api.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ru.spvb.trading.domain.order.OrderTerms;
import ru.spvb.trading.domain.order.Side;

import java.math.BigDecimal;

public record ReplaceOrderRequest(
                                  @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal price,
                                  @NotNull @Min(1) @Max(1_000_000_000_000L) Long quantity) {

    public OrderTerms terms(Side type) {
        return new OrderTerms(type, price, quantity);
    }
}
