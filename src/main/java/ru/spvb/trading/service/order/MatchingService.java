package ru.spvb.trading.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.spvb.trading.domain.TradingException;
import ru.spvb.trading.domain.order.Order;
import ru.spvb.trading.domain.order.Side;
import ru.spvb.trading.persistence.repository.OrderRepository;
import ru.spvb.trading.persistence.specification.OrderSpecifications;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class MatchingService {
    private final OrderRepository orders;
    private final SettlementService settlement;

    public void match(Order incoming) {
        while (incoming.isActive()) {
            Optional<Order> candidate = bestCounterparty(incoming);
            if (candidate.isEmpty()) {
                return;
            }
            Order resting = candidate.get();
            if (resting.getAccount().getUserId() == incoming.getAccount().getUserId()) {
                throw TradingException.conflict("SELF_TRADE", "Заявка приводит к сделке между счетами одного пользователя");
            }
            settlement.execute(incoming, resting);
        }
    }

    private Optional<Order> bestCounterparty(Order incoming) {
        Sort.Direction direction = incoming.getType() == Side.BUY ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort priority = Sort.by(direction, "price").and(Sort.by("id"));
        return orders.findAll(OrderSpecifications.counterparty(incoming), PageRequest.of(0, 1, priority))
                .stream().findFirst();
    }
}
