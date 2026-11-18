package ru.spvb.trading;

import org.junit.jupiter.api.Test;
import ru.spvb.trading.api.dto.order.CreateOrderRequest;
import ru.spvb.trading.api.dto.order.OrderResponse;
import ru.spvb.trading.domain.TradingException;
import ru.spvb.trading.domain.order.OrderStatus;
import ru.spvb.trading.domain.order.Side;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentTradingIntegrationTest extends TradingTestSupport {
    @Test
    void competingRequestsCannotSpendSameCashOrSellSameSecurities() throws Exception {
        List<Object> buys = race(() -> buy("100", 600),
                () -> orders.create(1, new CreateOrderRequest(101L, "GAZP", Side.BUY, bd("100"), 600L)));
        assertThat(buys.stream().filter(OrderResponse.class::isInstance)).hasSize(1);
        assertThat(buys.stream().filter(TradingException.class::isInstance)
                .map(TradingException.class::cast)).singleElement().extracting(TradingException::getCode)
                .isEqualTo("INSUFFICIENT_FUNDS");
        List<Object> sells = race(() -> sell(2, 201, "200", 600), () -> sell(2, 201, "200", 600));
        assertThat(sells.stream().filter(OrderResponse.class::isInstance)).hasSize(1);
        assertThat(sells.stream().filter(TradingException.class::isInstance)
                .map(TradingException.class::cast)).singleElement().extracting(TradingException::getCode)
                .isEqualTo("INSUFFICIENT_SECURITIES");
        assertConservation();
        assertReservationConsistency();
    }

    @Test
    void competingBuyersCannotExecuteSameRestingQuantityTwice() throws Exception {
        sell(2, 201, "100", 100);
        List<Object> results = race(() -> buy("100", 100),
                () -> orders.create(3, new CreateOrderRequest(301L, "SBER", Side.BUY, bd("100"), 100L)));
        assertThat(results).allMatch(OrderResponse.class::isInstance);
        assertThat(results.stream().map(OrderResponse.class::cast).map(OrderResponse::status))
                .containsExactlyInAnyOrder(OrderStatus.FILLED, OrderStatus.ACTIVE);
        assertThat(database.tradedQuantity()).isEqualTo(100);
        assertThat(database.position(201, "SBER").quantity()).isEqualTo(900);
        assertConservation();
        assertReservationConsistency();
    }

    @Test
    void cancellationRacingWithExecutionProducesOneConsistentOutcome() throws Exception {
        OrderResponse sale = sell(2, 201, "100", 100);
        List<Object> results = race(() -> orders.cancel(2, sale.id()), () -> buy("100", 100));
        assertThat(results.get(1)).isInstanceOf(OrderResponse.class);
        OrderResponse finalSale = database.order(sale.id());
        if (finalSale.status() == OrderStatus.FILLED) {
            assertThat(results.getFirst()).isInstanceOf(TradingException.class);
            assertThat(trades(1)).hasSize(1);
            assertThat(database.position(201, "SBER").quantity()).isEqualTo(900);
        } else {
            assertThat(finalSale.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(results.getFirst()).isInstanceOf(OrderResponse.class);
            assertThat(trades(1)).isEmpty();
            assertThat(database.position(201, "SBER").quantity()).isEqualTo(1000);
        }
        assertConservation();
        assertReservationConsistency();
    }
}
