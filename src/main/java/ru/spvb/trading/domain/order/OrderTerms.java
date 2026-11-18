package ru.spvb.trading.domain.order;

import ru.spvb.trading.domain.TradingException;
import ru.spvb.trading.domain.account.Account;
import ru.spvb.trading.domain.market.Instrument;

import java.math.BigDecimal;

public record OrderTerms(Side type, BigDecimal price, long quantity) {
    public void validate(Account account, Instrument instrument) {
        if (quantity % instrument.getLotSize() != 0) {
            throw TradingException.invalid("Количество должно быть кратно размеру лота: " + instrument.getLotSize());
        }
        if (price.remainder(instrument.getMinStep()).signum() != 0) {
            throw TradingException.invalid("Цена должна быть кратна шагу цены: " + instrument.getMinStep());
        }
        if (!account.getCurrency().equals(instrument.getCurrency())) {
            throw TradingException.invalid("Валюта счета должна совпадать с валютой расчетов инструмента");
        }
    }
}
