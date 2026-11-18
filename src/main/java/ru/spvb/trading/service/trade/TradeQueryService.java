package ru.spvb.trading.service.trade;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.spvb.trading.api.dto.common.PageResponse;
import ru.spvb.trading.api.dto.trade.TradeFilter;
import ru.spvb.trading.api.dto.trade.TradeResponse;
import ru.spvb.trading.persistence.repository.TradeRepository;
import ru.spvb.trading.persistence.specification.TradeSpecifications;
import ru.spvb.trading.service.TradingAccess;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class TradeQueryService {
    private final TradingAccess access;
    private final TradeRepository trades;

    public PageResponse<TradeResponse> trades(long userId, @Valid TradeFilter filter) {
        access.validateFilter(userId, filter);
        return PageResponse.from(trades.findAll(TradeSpecifications.register(userId, filter),
                filter.pageRequest("executedAt")).map(TradeResponse::from));
    }
}
