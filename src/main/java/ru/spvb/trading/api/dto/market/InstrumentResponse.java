package ru.spvb.trading.api.dto.market;

import ru.spvb.trading.domain.market.Instrument;

import java.math.BigDecimal;

public record InstrumentResponse(String seccode, String type, long lotSize, BigDecimal minStep, String currency) {
    public static InstrumentResponse from(Instrument instrument) {
        return new InstrumentResponse(instrument.getSeccode(), instrument.getType(), instrument.getLotSize(),
                instrument.getMinStep(), instrument.getCurrency());
    }
}
