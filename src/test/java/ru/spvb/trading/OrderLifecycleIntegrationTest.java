package ru.spvb.trading;

import org.junit.jupiter.api.Test;
import ru.spvb.trading.api.dto.market.OrderBookResponse;
import ru.spvb.trading.api.dto.market.PriceLevel;
import ru.spvb.trading.api.dto.order.CreateOrderRequest;
import ru.spvb.trading.api.dto.order.OrderResponse;
import ru.spvb.trading.api.dto.order.ReplaceOrderRequest;
import ru.spvb.trading.api.dto.trade.TradeResponse;
import ru.spvb.trading.domain.TradingException;
import ru.spvb.trading.domain.order.OrderStatus;
import ru.spvb.trading.domain.order.Side;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLifecycleIntegrationTest extends TradingTestSupport {
    @Test
    void sweepsBestPricesThenFifoAndPreservesAssets() {
        OrderResponse expensive = sell(2, 201, "102", 20);
        OrderResponse firstAtBest = sell(3, 301, "100", 20);
        OrderResponse secondAtBest = sell(2, 201, "100", 30);
        OrderResponse buy = buy("103", 60);

        List<TradeResponse> trades = trades(1).stream().sorted(Comparator.comparingLong(TradeResponse::id)).toList();
        assertThat(trades).extracting(TradeResponse::sellOrderId)
                .containsExactly(firstAtBest.id(), secondAtBest.id(), expensive.id());
        assertThat(trades).extracting(TradeResponse::quantity).containsExactly(20L, 30L, 10L);
        assertThat(trades).extracting(t -> t.price().stripTrailingZeros())
                .containsExactly(bd("100"), bd("100"), bd("102"));
        assertThat(buy.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(database.account(101).balance()).isEqualByComparingTo("93980");
        assertThat(database.account(101).reserved()).isZero();
        assertThat(database.position(101, "SBER").quantity()).isEqualTo(60);
        assertThat(database.order(expensive.id()).remainingQuantity()).isEqualTo(10);
        OrderBookResponse book = marketData.book("SBER", 20);
        assertThat(book.bids()).isEmpty();
        assertThat(book.asks()).singleElement().satisfies(level -> {
            assertThat(level.price()).isEqualByComparingTo("102");
            assertThat(level.quantity()).isEqualTo(10);
        });
        assertConservation();
        assertReservationConsistency();
    }

    @Test
    void executesIncomingSaleAtRestingBuyPriceAndReleasesOnlyCancelledRemainder() {
        OrderResponse buy = buy("110", 50);
        OrderResponse sell = sell(2, 201, "100", 20);
        assertThat(sell.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(trades(1)).singleElement().satisfies(t -> assertThat(t.price()).isEqualByComparingTo("110"));
        assertThat(database.account(101).balance()).isEqualByComparingTo("97800");
        assertThat(database.account(101).reserved()).isEqualByComparingTo("3300");
        OrderResponse cancelled = orders.cancel(1, buy.id());
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.filledQuantity()).isEqualTo(20);
        assertThat(orders.cancel(1, buy.id())).isEqualTo(cancelled);
        assertThat(database.account(101).reserved()).isZero();
        assertThat(database.account(101).balance()).isEqualByComparingTo("97800");
        assertThatThrownBy(() -> orders.cancel(2, sell.id())).isInstanceOf(TradingException.class)
                .extracting("code").isEqualTo("ORDER_NOT_ACTIVE");
        assertConservation();
        assertReservationConsistency();
    }

    @Test
    void nonCrossingBookAggregatesRemainingVolumesAndCancellationReleasesSecurities() {
        OrderResponse first = sell(2, 201, "100", 50);
        sell(3, 301, "100", 20);
        buy("99", 10);
        orders.create(1, new CreateOrderRequest(101L, "GAZP", Side.BUY, bd("110"), 10L));

        assertThat(marketData.book("GAZP", 1).bestBid()).isEqualByComparingTo("110");
        assertThat(marketData.book("GAZP", 1).asks()).isEmpty();
        OrderBookResponse book = marketData.book("SBER", 1);
        assertThat(book.bestBid()).isEqualByComparingTo("99");
        assertThat(book.bestAsk()).isEqualByComparingTo("100");
        assertThat(book.asks()).singleElement().extracting(PriceLevel::quantity).isEqualTo(70L);
        assertThat(trades(1)).isEmpty();
        buy("100", 20);
        assertThat(database.position(201, "SBER").reserved()).isEqualTo(30);
        orders.cancel(2, first.id());
        assertThat(database.position(201, "SBER").quantity()).isEqualTo(980);
        assertThat(database.position(201, "SBER").reserved()).isZero();
        assertThat(marketData.book("SBER", 1).asks()).singleElement().extracting(PriceLevel::quantity).isEqualTo(20L);
        assertConservation();
        assertReservationConsistency();
    }

    @Test
    void replacementRollsBackOnFailureAndLosesTimePriorityOnSuccess() {
        OrderResponse original = sell(2, 201, "100", 50);
        buy("100", 20);
        OrderResponse partial = database.order(original.id());
        assertThat(partial.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThatThrownBy(() -> orders.replace(2, original.id(), new ReplaceOrderRequest(bd("100"), 2000L)))
                .isInstanceOf(TradingException.class).extracting("code").isEqualTo("INSUFFICIENT_SECURITIES");
        assertThat(database.order(original.id())).isEqualTo(partial);
        assertThat(database.position(201, "SBER").reserved()).isEqualTo(30);

        OrderResponse competitor = sell(3, 301, "100", 10);
        OrderResponse replacement = orders.replace(2, original.id(), new ReplaceOrderRequest(bd("100"), 20L));
        assertThat(replacement.replacesOrderId()).isEqualTo(original.id());
        assertThat(replacement.quantity()).isEqualTo(20);
        assertThat(replacement.filledQuantity()).isZero();
        assertThat(database.order(original.id()).status()).isEqualTo(OrderStatus.CANCELLED);
        buy("100", 20);
        assertThat(database.order(competitor.id()).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(database.order(replacement.id()).filledQuantity()).isEqualTo(10);
        assertConservation();
        assertReservationConsistency();
    }

    @Test
    void replacementCanReuseCashButFailedIncreasePreservesOldReservation() {
        OrderResponse original = buy("100", 900);
        assertThatThrownBy(() -> orders.replace(1, original.id(), new ReplaceOrderRequest(bd("200"), 900L)))
                .isInstanceOf(TradingException.class).extracting("code").isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(database.order(original.id())).isEqualTo(original);
        assertThat(database.account(101).reserved()).isEqualByComparingTo("90000");
        OrderResponse replacement = orders.replace(1, original.id(), new ReplaceOrderRequest(bd("105"), 900L));
        assertThat(database.account(101).reserved()).isEqualByComparingTo("94500");
        assertThat(replacement.status()).isEqualTo(OrderStatus.ACTIVE);
        assertReservationConsistency();
    }

    @Test
    void reachingOwnCounterpartyRollsBackEarlierMatchesAndReplacement() {
        OrderResponse external = sell(2, 201, "100", 10);
        OrderResponse own = sell(1, 102, "101", 10);
        OrderResponse original = buy("99", 20);
        assertThatThrownBy(() -> orders.replace(1, original.id(), new ReplaceOrderRequest(bd("102"), 20L)))
                .isInstanceOf(TradingException.class).extracting("code").isEqualTo("SELF_TRADE");
        assertThat(database.order(original.id())).isEqualTo(original);
        assertThat(database.order(external.id())).isEqualTo(external);
        assertThat(database.order(own.id())).isEqualTo(own);
        assertThat(trades(1)).isEmpty();
        assertThat(database.account(101).reserved()).isEqualByComparingTo("1980");
        assertConservation();
        assertReservationConsistency();
    }
}
