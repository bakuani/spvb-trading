package ru.spvb.trading.service.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.spvb.trading.api.dto.account.AccountResponse;
import ru.spvb.trading.api.dto.account.PositionResponse;
import ru.spvb.trading.persistence.repository.AccountRepository;
import ru.spvb.trading.persistence.repository.PositionRepository;
import ru.spvb.trading.service.TradingAccess;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class AccountQueryService {
    private final TradingAccess access;
    private final AccountRepository accounts;
    private final PositionRepository positions;

    public List<AccountResponse> accounts(long userId) {
        return accounts.findByUserIdOrderById(userId).stream().map(AccountResponse::from).toList();
    }

    public AccountResponse account(long userId, long id) {
        return AccountResponse.from(access.account(userId, id));
    }

    public List<PositionResponse> positions(long userId, long accountId) {
        access.account(userId, accountId);
        return positions.findByIdAccountIdOrderByIdSeccode(accountId).stream().map(PositionResponse::from).toList();
    }
}
