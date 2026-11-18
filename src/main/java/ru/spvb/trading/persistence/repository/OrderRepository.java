package ru.spvb.trading.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.spvb.trading.api.dto.market.PriceLevel;
import ru.spvb.trading.domain.order.Order;
import ru.spvb.trading.domain.order.OrderStatus;
import ru.spvb.trading.domain.order.Side;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @Override
    @EntityGraph(attributePaths = {"account", "instrument"})
    Page<Order> findAll(Specification<Order> specification, Pageable pageable);

    @Query("""
            select new ru.spvb.trading.api.dto.market.PriceLevel(o.price, sum(o.quantity - o.filledQuantity))
            from TradeOrder o
            where o.instrument.seccode = :seccode and o.type = :side and o.status in :statuses
            group by o.price
            """)
    List<PriceLevel> findPriceLevels(String seccode, Side side, Collection<OrderStatus> statuses, Pageable pageable);
}
