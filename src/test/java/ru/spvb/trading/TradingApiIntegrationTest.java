package ru.spvb.trading;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.spvb.trading.api.dto.order.CreateOrderRequest;
import ru.spvb.trading.api.dto.order.OrderResponse;
import ru.spvb.trading.api.dto.trade.TradeResponse;
import ru.spvb.trading.domain.TradingException;
import ru.spvb.trading.domain.order.Side;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TradingApiIntegrationTest extends TradingTestSupport {
    @Test
    void httpValidationAndOwnershipFailuresDoNotMutateState() throws Exception {
        List<String> invalid = List.of(
                "{\"accountId\":101,\"seccode\":\"SBER\",\"type\":\"BUY\",\"price\":100.03,\"quantity\":10}",
                "{\"accountId\":101,\"seccode\":\"SBER\",\"type\":\"BUY\",\"price\":100,\"quantity\":11}",
                "{\"accountId\":101,\"seccode\":\"SBER\",\"type\":\"BUY\",\"price\":0,\"quantity\":10}",
                "{\"accountId\":101,\"seccode\":\"SBER\",\"type\":\"BUY\",\"price\":100.00001,\"quantity\":10}",
                "{\"accountId\":101,\"seccode\":\"SBER\",\"type\":\"BUY\",\"price\":100,\"quantity\":10.5}",
                "{\"accountId\":101,\"seccode\":\"SBER\",\"type\":\"BUY\",\"price\":100,\"quantity\":10,\"unexpected\":1}");
        for (String body : invalid) {
            mvc.perform(post("/api/v1/orders").header("X-User-Id", 1).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        mvc.perform(post("/api/v1/orders").header("X-User-Id", 2).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(new CreateOrderRequest(202L, "SBER", Side.BUY, bd("100"), 10L))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/orders").header("X-User-Id", 1).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(new CreateOrderRequest(201L, "SBER", Side.BUY, bd("100"), 10L))))
                .andExpect(status().isNotFound());
        assertThatThrownBy(() -> buy("100", 1010)).isInstanceOf(TradingException.class)
                .extracting("code").isEqualTo("INSUFFICIENT_FUNDS");
        assertThatThrownBy(() -> sell(2, 201, "100", 1010)).isInstanceOf(TradingException.class)
                .extracting("code").isEqualTo("INSUFFICIENT_SECURITIES");
        assertThat(database.orderCount()).isZero();
        assertConservation();
        assertReservationConsistency();
    }

    @Test
    void restLifecycleReturnsLocationBalancesAndPrivateFilteredRegisters() throws Exception {
        OrderResponse sale = sell(2, 201, "100", 30);
        Instant from = Instant.now().minusSeconds(1);
        String json = mvc.perform(post("/api/v1/orders").header("X-User-Id", 1).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(new CreateOrderRequest(101L, "SBER", Side.BUY, bd("101"), 20L))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.remainingQuantity").value(0)).andReturn().getResponse().getContentAsString();
        long buyId = mapper.readTree(json).get("id").asLong();
        buy("99", 10);
        mvc.perform(get("/api/v1/orders").header("X-User-Id", 1).param("status", "FILLED")
                        .param("accountId", "101").param("seccode", "SBER").param("from", from.toString())
                        .param("to", Instant.now().plusSeconds(1).toString()).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(buyId));
        mvc.perform(get("/api/v1/orders").header("X-User-Id", 1).param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].id").value(buyId));
        mvc.perform(get("/api/v1/orders/{id}", sale.id()).header("X-User-Id", 1)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/orders/{id}", sale.id()).header("X-User-Id", 1)).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/orders/{id}", sale.id()).header("X-User-Id", 1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":100,\"quantity\":10}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/trades").header("X-User-Id", 3)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/trades").header("X-User-Id", 2).param("accountId", "201").param("seccode", "SBER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/trades").header("X-User-Id", 1).param("accountId", "201"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/accounts/101").header("X-User-Id", 1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(97010));
        mvc.perform(get("/api/v1/orders")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/orders").header("X-User-Id", 1).param("size", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/orders").header("X-User-Id", 1).param("from", "2026-02-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z")).andExpect(status().isBadRequest());
        assertConservation();
        assertReservationConsistency();
    }

    @Test
    void openApiDescribesOperationsAndRequiredUserHeader() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode paths = mapper.readTree(json).path("paths");
        assertThat(paths.has("/api/v1/instruments/{seccode}/orderbook")).isTrue();
        assertThat(paths.path("/api/v1/orders/{id}").has("put")).isTrue();
        assertThat(paths.path("/api/v1/orders").path("post").path("parameters"))
                .anySatisfy(parameter -> {
                    assertThat(parameter.path("name").asText()).isEqualTo("X-User-Id");
                    assertThat(parameter.path("required").asBoolean()).isTrue();
                });
        assertThat(paths.path("/api/v1/orders").path("get").path("parameters"))
                .extracting(parameter -> parameter.path("name").asText())
                .containsExactlyInAnyOrder("X-User-Id", "accountId", "seccode", "status", "from", "to", "page", "size");
        assertThat(paths.path("/api/v1/trades").path("get").path("parameters"))
                .extracting(parameter -> parameter.path("name").asText())
                .containsExactlyInAnyOrder("X-User-Id", "accountId", "seccode", "from", "to", "page", "size");
    }

    @Test
    void filterDtosPreserveDefaultsRejectInvalidValuesAndApplyTradePeriod() throws Exception {
        sell(2, 201, "100", 30);
        buy("100", 20);
        TradeResponse trade = trades(1).getFirst();

        mvc.perform(get("/api/v1/trades").header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
        mvc.perform(get("/api/v1/trades").header("X-User-Id", 1)
                        .param("from", trade.executedAt().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/trades").header("X-User-Id", 1)
                        .param("to", trade.executedAt().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/trades").header("X-User-Id", 1).param("seccode", "GAZP"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));

        for (String register : List.of("/api/v1/orders", "/api/v1/trades")) {
            for (var invalid : Map.of(
                    "page", "-1", "size", "101", "accountId", "0", "seccode", "invalid!", "from", "not-a-date").entrySet()) {
                mvc.perform(get(register).header("X-User-Id", 1).param(invalid.getKey(), invalid.getValue()))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
            }
        }
        mvc.perform(get("/api/v1/orders").header("X-User-Id", 1).param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }
}
