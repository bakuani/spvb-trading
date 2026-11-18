package ru.spvb.trading.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.spvb.trading.api.dto.common.PageResponse;
import ru.spvb.trading.api.dto.trade.TradeFilter;
import ru.spvb.trading.api.dto.trade.TradeResponse;
import ru.spvb.trading.service.trade.TradeQueryService;

@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
@Tag(name = "Сделки")
public class TradeController {
    private final TradeQueryService queries;

    @GetMapping
    @Operation(summary = "Реестр сделок со своими счетами; период [from, to) по времени исполнения")
    public PageResponse<TradeResponse> trades(@RequestHeader("X-User-Id") @Positive long userId,
            @ModelAttribute @Valid @ParameterObject TradeFilter filter) {
        return queries.trades(userId, filter);
    }
}
