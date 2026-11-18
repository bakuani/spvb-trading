package ru.spvb.trading.api;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.spvb.trading.domain.TradingException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(TradingException.class)
    public ResponseEntity<ProblemDetail> trading(TradingException exception) {
        return problem(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> body(MethodArgumentNotValidException exception) {
        var result = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Некорректные параметры запроса");
        result.getBody().setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).sorted().toList());
        return result;
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ProblemDetail> parameters() {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Проверьте JSON, заголовок X-User-Id и параметры запроса");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> missing() {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ресурс не найден");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> method() {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Метод не поддерживается");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> mediaType() {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Тело запроса должно иметь тип application/json");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetail> accept() {
        return problem(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", "Запрошенный формат ответа не поддерживается");
    }

    @ExceptionHandler(TransientDataAccessException.class)
    public ResponseEntity<ProblemDetail> busy(TransientDataAccessException exception) {
        log.warn("Transient database failure", exception);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "TEMPORARILY_UNAVAILABLE", "Временная ошибка базы данных");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Внутренняя ошибка сервиса");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
