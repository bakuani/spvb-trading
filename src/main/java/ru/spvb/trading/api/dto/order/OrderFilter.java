package ru.spvb.trading.api.dto.order;

import lombok.Getter;
import lombok.Setter;
import ru.spvb.trading.api.dto.common.RegisterFilter;
import ru.spvb.trading.domain.order.OrderStatus;

@Getter
@Setter
public class OrderFilter extends RegisterFilter {
    private OrderStatus status;
}
