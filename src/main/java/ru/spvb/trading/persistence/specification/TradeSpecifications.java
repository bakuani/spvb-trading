package ru.spvb.trading.persistence.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.spvb.trading.api.dto.trade.TradeFilter;
import ru.spvb.trading.domain.trade.Trade;

public final class TradeSpecifications {
    private TradeSpecifications() {
    }

    public static Specification<Trade> register(long userId, TradeFilter filter) {
        return (root, query, builder) -> {
            var buyer = root.get("buyOrder").get("account");
            var seller = root.get("sellOrder").get("account");
            var predicates = RegisterPredicates.from(filter, root, "executedAt", builder);
            predicates.add(builder.or(
                    builder.equal(buyer.get("userId"), userId),
                    builder.equal(seller.get("userId"), userId)));
            if (filter.getAccountId() != null) {
                predicates.add(builder.or(
                        builder.equal(buyer.get("id"), filter.getAccountId()),
                        builder.equal(seller.get("id"), filter.getAccountId())));
            }
            if (filter.getSeccode() != null) {
                predicates.add(builder.equal(root.get("seccode"), filter.getSeccode()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
