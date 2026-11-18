package ru.spvb.trading.api.dto.account;

import ru.spvb.trading.domain.account.Position;

public record PositionResponse(long accountId, String seccode, long quantity, long reserved, long available) {
    public static PositionResponse from(Position position) {
        return new PositionResponse(position.getId().getAccountId(), position.getId().getSeccode(),
                position.getQuantity(), position.getReserved(), position.available());
    }
}
