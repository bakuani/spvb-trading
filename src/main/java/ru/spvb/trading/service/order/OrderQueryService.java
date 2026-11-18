package ru.spvb.trading.service.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.spvb.trading.api.dto.common.PageResponse;
import ru.spvb.trading.api.dto.order.OrderFilter;
import ru.spvb.trading.api.dto.order.OrderResponse;
import ru.spvb.trading.persistence.repository.OrderRepository;
import ru.spvb.trading.persistence.specification.OrderSpecifications;
import ru.spvb.trading.service.TradingAccess;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class OrderQueryService {
    private final TradingAccess access;
    private final OrderRepository orders;

    public OrderResponse order(long userId, long id) {
        return OrderResponse.from(access.order(userId, id));
    }

    public PageResponse<OrderResponse> orders(long userId, @Valid OrderFilter filter) {
        access.validateFilter(userId, filter);
        return PageResponse.from(orders.findAll(OrderSpecifications.register(userId, filter),
                filter.pageRequest("createdAt")).map(OrderResponse::from));
    }
}
