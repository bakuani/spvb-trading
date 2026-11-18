package ru.spvb.trading.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.spvb.trading.api.dto.common.RegisterFilter;
import ru.spvb.trading.domain.TradingException;
import ru.spvb.trading.domain.account.Account;
import ru.spvb.trading.domain.market.Instrument;
import ru.spvb.trading.domain.order.Order;
import ru.spvb.trading.persistence.repository.AccountRepository;
import ru.spvb.trading.persistence.repository.InstrumentRepository;
import ru.spvb.trading.persistence.repository.OrderRepository;

@Component
@RequiredArgsConstructor
public class TradingAccess {
    private final AccountRepository accounts;
    private final InstrumentRepository instruments;
    private final OrderRepository orders;

    public Account account(long userId, long accountId) {
        return accounts.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> TradingException.missing("Счет"));
    }

    public Instrument instrument(String seccode) {
        return instruments.findById(seccode)
                .orElseThrow(() -> TradingException.missing("Инструмент"));
    }

    public Order order(long userId, long orderId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> TradingException.missing("Заявка"));
        if (order.getAccount().getUserId() != userId) {
            throw TradingException.missing("Заявка");
        }
        return order;
    }

    public void validateFilter(long userId, RegisterFilter filter) {
        if (filter.getAccountId() != null) {
            account(userId, filter.getAccountId());
        }
        if (filter.getSeccode() != null) {
            instrument(filter.getSeccode());
        }
    }
}
