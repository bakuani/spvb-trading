package ru.spvb.trading;

import org.springframework.transaction.annotation.Transactional;
import ru.spvb.trading.api.dto.account.AccountResponse;
import ru.spvb.trading.api.dto.account.PositionResponse;
import ru.spvb.trading.api.dto.order.OrderResponse;
import ru.spvb.trading.domain.account.PositionId;
import ru.spvb.trading.domain.order.OrderStatus;
import ru.spvb.trading.domain.order.Side;
import ru.spvb.trading.domain.trade.Trade;
import ru.spvb.trading.persistence.repository.AccountRepository;
import ru.spvb.trading.persistence.repository.OrderRepository;
import ru.spvb.trading.persistence.repository.PositionRepository;
import ru.spvb.trading.persistence.repository.TradeRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(readOnly = true)
public class DatabaseState {
    private final AccountRepository accounts;
    private final PositionRepository positions;
    private final OrderRepository orders;
    private final TradeRepository trades;

    public DatabaseState(AccountRepository accounts, PositionRepository positions,
                         OrderRepository orders, TradeRepository trades) {
        this.accounts = accounts;
        this.positions = positions;
        this.orders = orders;
        this.trades = trades;
    }

    public AccountResponse account(long id) {
        return AccountResponse.from(accounts.findById(id).orElseThrow());
    }

    public PositionResponse position(long accountId, String seccode) {
        return positions.findById(new PositionId(accountId, seccode)).map(PositionResponse::from)
                .orElse(new PositionResponse(accountId, seccode, 0, 0, 0));
    }

    public OrderResponse order(long id) {
        return OrderResponse.from(orders.findById(id).orElseThrow());
    }

    public long orderCount() {
        return orders.count();
    }

    public long tradedQuantity() {
        return trades.findAll().stream().mapToLong(Trade::getQuantity).sum();
    }

    public void assertConservation() {
        assertThat(totalBalance("RUB")).isEqualByComparingTo("120000");
        assertThat(totalBalance("CNY")).isEqualByComparingTo("10000");
        assertThat(totalQuantity("SBER")).isEqualTo(3000);
        assertThat(totalQuantity("GAZP")).isEqualTo(1000);
    }

    public void assertReservationConsistency() {
        List<OrderResponse> active = orders.findAll().stream().map(OrderResponse::from)
                .filter(order -> order.status() == OrderStatus.ACTIVE || order.status() == OrderStatus.PARTIALLY_FILLED)
                .toList();
        for (var account : accounts.findAll()) {
            BigDecimal expected = active.stream()
                    .filter(order -> order.accountId() == account.getId() && order.type() == Side.BUY)
                    .map(order -> order.price().multiply(BigDecimal.valueOf(order.quantity() - order.filledQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(account.getReserved()).as("Cash reserved on account %s", account.getId())
                    .isEqualByComparingTo(expected);
        }
        for (var position : positions.findAll()) {
            long expected = active.stream()
                    .filter(order -> order.accountId() == position.getId().getAccountId()
                            && order.seccode().equals(position.getId().getSeccode()) && order.type() == Side.SELL)
                    .mapToLong(order -> order.quantity() - order.filledQuantity()).sum();
            assertThat(position.getReserved()).as("Securities reserved on account %s for %s",
                    position.getId().getAccountId(), position.getId().getSeccode()).isEqualTo(expected);
        }
        active.stream().filter(order -> order.type() == Side.SELL).forEach(order ->
                assertThat(positions.existsById(new PositionId(order.accountId(), order.seccode()))).isTrue());
    }

    private BigDecimal totalBalance(String currency) {
        return accounts.findAll().stream().filter(account -> account.getCurrency().equals(currency))
                .map(account -> account.getBalance()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long totalQuantity(String seccode) {
        return positions.findAll().stream().filter(position -> position.getId().getSeccode().equals(seccode))
                .mapToLong(position -> position.getQuantity()).sum();
    }
}
