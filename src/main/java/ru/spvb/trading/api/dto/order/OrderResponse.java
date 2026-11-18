package ru.spvb.trading.api.dto.order;

import ru.spvb.trading.domain.order.Order;
import ru.spvb.trading.domain.order.OrderStatus;
import ru.spvb.trading.domain.order.Side;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(long id, long accountId, String seccode, Side type, BigDecimal price,
                            long quantity, long filledQuantity, OrderStatus status, Instant createdAt,
                            Instant updatedAt, Long replacesOrderId, long remainingQuantity) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getAccount().getId(), order.getInstrument().getSeccode(),
                order.getType(), order.getPrice(), order.getQuantity(), order.getFilledQuantity(),
                order.getStatus(), order.getCreatedAt(), order.getUpdatedAt(), order.getReplacesOrderId(),
                order.remainingQuantity());
    }
}
