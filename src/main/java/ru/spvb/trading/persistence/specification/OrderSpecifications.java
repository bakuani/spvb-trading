package ru.spvb.trading.persistence.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.spvb.trading.api.dto.order.OrderFilter;
import ru.spvb.trading.domain.order.Order;
import ru.spvb.trading.domain.order.OrderStatus;
import ru.spvb.trading.domain.order.Side;

import java.util.List;

public final class OrderSpecifications {
    public static final List<OrderStatus> ACTIVE_STATUSES = List.of(OrderStatus.ACTIVE, OrderStatus.PARTIALLY_FILLED);

    private OrderSpecifications() {
    }

    public static Specification<Order> register(long userId, OrderFilter filter) {
        return (root, query, builder) -> {
            var predicates = RegisterPredicates.from(filter, root, "createdAt", builder);
            predicates.add(builder.equal(root.get("account").get("userId"), userId));
            if (filter.getAccountId() != null) {
                predicates.add(builder.equal(root.get("account").get("id"), filter.getAccountId()));
            }
            if (filter.getSeccode() != null) {
                predicates.add(builder.equal(root.get("instrument").get("seccode"), filter.getSeccode()));
            }
            if (filter.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), filter.getStatus()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Order> counterparty(Order incoming) {
        return (root, query, builder) -> builder.and(
                builder.equal(root.get("instrument").get("seccode"), incoming.getInstrument().getSeccode()),
                builder.notEqual(root.get("type"), incoming.getType()),
                root.get("status").in(ACTIVE_STATUSES),
                incoming.getType() == Side.BUY
                        ? builder.lessThanOrEqualTo(root.get("price"), incoming.getPrice())
                        : builder.greaterThanOrEqualTo(root.get("price"), incoming.getPrice()));
    }
}
