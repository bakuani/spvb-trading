package ru.spvb.trading.service.market;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.spvb.trading.api.dto.market.InstrumentResponse;
import ru.spvb.trading.api.dto.market.OrderBookResponse;
import ru.spvb.trading.domain.order.Side;
import ru.spvb.trading.persistence.repository.InstrumentRepository;
import ru.spvb.trading.persistence.repository.OrderRepository;
import ru.spvb.trading.service.TradingAccess;

import java.util.List;

import static ru.spvb.trading.persistence.specification.OrderSpecifications.ACTIVE_STATUSES;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class MarketDataService {
    private final TradingAccess access;
    private final InstrumentRepository instruments;
    private final OrderRepository orders;

    public List<InstrumentResponse> instruments() {
        return instruments.findAll(Sort.by("seccode")).stream().map(InstrumentResponse::from).toList();
    }

    public OrderBookResponse book(String seccode, int depth) {
        access.instrument(seccode);
        var bids = orders.findPriceLevels(seccode, Side.BUY, ACTIVE_STATUSES,
                PageRequest.of(0, depth, Sort.by(Sort.Direction.DESC, "price")));
        var asks = orders.findPriceLevels(seccode, Side.SELL, ACTIVE_STATUSES,
                PageRequest.of(0, depth, Sort.by(Sort.Direction.ASC, "price")));
        return OrderBookResponse.of(seccode, bids, asks);
    }
}
