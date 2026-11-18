package ru.spvb.trading.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.spvb.trading.api.dto.common.PageResponse;
import ru.spvb.trading.api.dto.order.CreateOrderRequest;
import ru.spvb.trading.api.dto.order.OrderFilter;
import ru.spvb.trading.api.dto.order.OrderResponse;
import ru.spvb.trading.api.dto.order.ReplaceOrderRequest;
import ru.spvb.trading.service.order.OrderQueryService;
import ru.spvb.trading.service.order.OrderService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Заявки")
public class OrderController {
    private final OrderService orders;
    private final OrderQueryService queries;

    @PostMapping
    @Operation(summary = "Выставить лимитную заявку")
    public ResponseEntity<OrderResponse> create(@RequestHeader("X-User-Id") @Positive long userId,
            @RequestBody @Valid CreateOrderRequest request) {
        return created(orders.create(userId, request));
    }

    @GetMapping("/{id}")
    public OrderResponse order(@RequestHeader("X-User-Id") @Positive long userId, @PathVariable @Positive long id) {
        return queries.order(userId, id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Отменить неисполненный остаток заявки")
    public OrderResponse cancel(@RequestHeader("X-User-Id") @Positive long userId, @PathVariable @Positive long id) {
        return orders.cancel(userId, id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Заменить остаток новой заявкой; quantity — новый остаток, приоритет сбрасывается")
    public ResponseEntity<OrderResponse> replace(@RequestHeader("X-User-Id") @Positive long userId,
            @PathVariable @Positive long id,
            @RequestBody @Valid ReplaceOrderRequest request) {
        return created(orders.replace(userId, id, request));
    }

    @GetMapping
    @Operation(summary = "Реестр своих заявок; период [from, to) по времени создания")
    public PageResponse<OrderResponse> orders(@RequestHeader("X-User-Id") @Positive long userId,
            @ModelAttribute @Valid @ParameterObject OrderFilter filter) {
        return queries.orders(userId, filter);
    }

    private ResponseEntity<OrderResponse> created(OrderResponse order) {
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id())).body(order);
    }
}
