package ru.spvb.trading.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.spvb.trading.domain.TradingException;
import ru.spvb.trading.domain.account.Position;
import ru.spvb.trading.domain.account.PositionId;
import ru.spvb.trading.domain.order.Order;
import ru.spvb.trading.domain.order.Side;
import ru.spvb.trading.domain.trade.Trade;
import ru.spvb.trading.persistence.repository.PositionRepository;
import ru.spvb.trading.persistence.repository.TradeRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class SettlementService {
    private final PositionRepository positions;
    private final TradeRepository trades;

    public void reserve(Order order) {
        if (order.getType() == Side.BUY) {
            order.getAccount().reserve(reservation(order));
        } else {
            sellerPosition(order).reserve(order.remainingQuantity());
        }
    }

    public void release(Order order) {
        if (order.getType() == Side.BUY) {
            order.getAccount().release(reservation(order));
        } else {
            sellerPosition(order).release(order.remainingQuantity());
        }
    }

    public void execute(Order incoming, Order resting) {
        Order buy = incoming.getType() == Side.BUY ? incoming : resting;
        Order sell = incoming.getType() == Side.SELL ? incoming : resting;
        long quantity = Math.min(buy.remainingQuantity(), sell.remainingQuantity());
        BigDecimal price = resting.getPrice();
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal reservedCash = buy.getPrice().multiply(BigDecimal.valueOf(quantity));

        buy.getAccount().pay(amount, reservedCash);
        sell.getAccount().receive(amount);
        sellerPosition(sell).deliver(quantity);
        buyerPosition(buy).receive(quantity);
        buy.fill(quantity);
        sell.fill(quantity);
        trades.save(new Trade(buy, sell, price, quantity));
    }

    private BigDecimal reservation(Order order) {
        return order.getPrice().multiply(BigDecimal.valueOf(order.remainingQuantity()));
    }

    private Position sellerPosition(Order order) {
        return positions.findById(positionId(order)).orElseThrow(() ->
                TradingException.conflict("INSUFFICIENT_SECURITIES", "Недостаточно свободных бумаг"));
    }

    private Position buyerPosition(Order order) {
        PositionId id = positionId(order);
        return positions.findById(id).orElseGet(() -> positions.save(new Position(id)));
    }

    private PositionId positionId(Order order) {
        return new PositionId(order.getAccount().getId(), order.getInstrument().getSeccode());
    }
}
