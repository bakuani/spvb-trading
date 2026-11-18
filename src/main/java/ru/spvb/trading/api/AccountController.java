package ru.spvb.trading.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.spvb.trading.api.dto.account.AccountResponse;
import ru.spvb.trading.api.dto.account.PositionResponse;
import ru.spvb.trading.service.account.AccountQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Счета")
public class AccountController {
    private final AccountQueryService queries;

    @GetMapping
    public List<AccountResponse> accounts(@RequestHeader("X-User-Id") @Positive long userId) {
        return queries.accounts(userId);
    }

    @GetMapping("/{id}")
    public AccountResponse account(@RequestHeader("X-User-Id") @Positive long userId,
            @PathVariable @Positive long id) {
        return queries.account(userId, id);
    }

    @GetMapping("/{id}/positions")
    public List<PositionResponse> positions(@RequestHeader("X-User-Id") @Positive long userId,
            @PathVariable @Positive long id) {
        return queries.positions(userId, id);
    }
}
