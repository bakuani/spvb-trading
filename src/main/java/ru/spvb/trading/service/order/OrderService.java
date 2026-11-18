package ru.spvb.trading.service.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.spvb.trading.api.dto.order.CreateOrderRequest;
import ru.spvb.trading.api.dto.order.OrderResponse;
import ru.spvb.trading.api.dto.order.ReplaceOrderRequest;
import ru.spvb.trading.domain.account.Account;
import ru.spvb.trading.domain.market.Instrument;
import ru.spvb.trading.domain.order.Order;
import ru.spvb.trading.domain.order.OrderStatus;
import ru.spvb.trading.domain.order.OrderTerms;
import ru.spvb.trading.persistence.locking.TradingLockRepository;
import ru.spvb.trading.persistence.repository.OrderRepository;
import ru.spvb.trading.service.TradingAccess;

@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final TradingLockRepository tradingLock;
    private final TradingAccess access;
    private final OrderRepository orders;
    private final SettlementService settlement;
    private final MatchingService matching;

    public OrderResponse create(long userId, @Valid CreateOrderRequest request) {
        tradingLock.acquire();
        Account account = access.account(userId, request.accountId());
        Instrument instrument = access.instrument(request.seccode());
        OrderTerms terms = request.terms();
        terms.validate(account, instrument);
        return place(new Order(account, instrument, terms));
    }

    public OrderResponse cancel(long userId, long id) {
        tradingLock.acquire();
        Order order = access.order(userId, id);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            cancelRemaining(order);
        }
        return OrderResponse.from(order);
    }

    public OrderResponse replace(long userId, long id, @Valid ReplaceOrderRequest request) {
        tradingLock.acquire();
        Order original = access.order(userId, id);
        OrderTerms terms = request.terms(original.getType());
        terms.validate(original.getAccount(), original.getInstrument());
        cancelRemaining(original);
        return place(original.replacement(terms));
    }

    private OrderResponse place(Order order) {
        settlement.reserve(order);
        orders.save(order);
        matching.match(order);
        return OrderResponse.from(order);
    }

    private void cancelRemaining(Order order) {
        order.requireActive();
        settlement.release(order);
        order.cancel();
    }
}
