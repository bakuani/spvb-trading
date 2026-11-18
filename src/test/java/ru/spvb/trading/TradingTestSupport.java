package ru.spvb.trading;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import ru.spvb.trading.api.dto.order.CreateOrderRequest;
import ru.spvb.trading.api.dto.order.OrderResponse;
import ru.spvb.trading.api.dto.trade.TradeFilter;
import ru.spvb.trading.api.dto.trade.TradeResponse;
import ru.spvb.trading.domain.TradingException;
import ru.spvb.trading.domain.order.Side;
import ru.spvb.trading.service.market.MarketDataService;
import ru.spvb.trading.service.order.OrderService;
import ru.spvb.trading.service.trade.TradeQueryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestDatabaseConfiguration.class, DatabaseState.class})
@Sql("/fixtures.sql")
abstract class TradingTestSupport {
    @Autowired
    protected OrderService orders;

    @Autowired
    protected MarketDataService marketData;

    @Autowired
    protected TradeQueryService tradeQueries;

    @Autowired
    protected DatabaseState database;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    protected OrderResponse buy(String price, long quantity) {
        return orders.create(1, new CreateOrderRequest(101L, "SBER", Side.BUY, bd(price), quantity));
    }

    protected OrderResponse sell(long user, long account, String price, long quantity) {
        return orders.create(user, new CreateOrderRequest(account, "SBER", Side.SELL, bd(price), quantity));
    }

    protected List<TradeResponse> trades(long user) {
        TradeFilter filter = new TradeFilter();
        filter.setSize(100);
        return tradeQueries.trades(user, filter).items();
    }

    protected static BigDecimal bd(String value) {
        return new BigDecimal(value).stripTrailingZeros();
    }

    protected List<Object> race(Supplier<?> first, Supplier<?> second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Object> task1 = concurrentTask(first, ready, start);
            Callable<Object> task2 = concurrentTask(second, ready, start);
            Future<Object> one = executor.submit(task1);
            Future<Object> two = executor.submit(task2);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        } finally {
            start.countDown();
        }
    }

    private Callable<Object> concurrentTask(Supplier<?> operation, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Race did not start");
            }
            try {
                return operation.get();
            } catch (TradingException expected) {
                return expected;
            }
        };
    }

    protected void assertConservation() {
        database.assertConservation();
    }

    protected void assertReservationConsistency() {
        database.assertReservationConsistency();
    }
}
