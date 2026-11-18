package ru.spvb.trading.domain;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TradingException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public TradingException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static TradingException invalid(String message) {
        return new TradingException(HttpStatus.BAD_REQUEST, "INVALID_ORDER", message);
    }

    public static TradingException missing(String resource) {
        return new TradingException(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + " не найден");
    }

    public static TradingException conflict(String code, String message) {
        return new TradingException(HttpStatus.CONFLICT, code, message);
    }
}
