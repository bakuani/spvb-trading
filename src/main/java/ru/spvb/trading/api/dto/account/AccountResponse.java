package ru.spvb.trading.api.dto.account;

import ru.spvb.trading.domain.account.Account;

import java.math.BigDecimal;

public record AccountResponse(long id, long userId, BigDecimal balance, BigDecimal reserved,
                              String currency, BigDecimal available) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getUserId(), account.getBalance(),
                account.getReserved(), account.getCurrency(), account.available());
    }
}
