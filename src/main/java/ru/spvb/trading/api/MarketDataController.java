package ru.spvb.trading.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.spvb.trading.api.dto.market.InstrumentResponse;
import ru.spvb.trading.api.dto.market.OrderBookResponse;
import ru.spvb.trading.service.market.MarketDataService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/instruments")
@RequiredArgsConstructor
@Tag(name = "Рыночные данные")
public class MarketDataController {
    private final MarketDataService marketData;

    @GetMapping
    public List<InstrumentResponse> instruments() {
        return marketData.instruments();
    }

    @GetMapping("/{seccode}/orderbook")
    @Operation(summary = "Стакан и лучшие цены; обновление периодическими REST-запросами")
    public OrderBookResponse book(@PathVariable @Pattern(regexp = "[A-Z0-9._-]{1,32}") String seccode,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int depth) {
        return marketData.book(seccode, depth);
    }
}
