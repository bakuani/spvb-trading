package ru.spvb.trading.api.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Getter
@Setter
public class RegisterFilter {
    @Positive
    private Long accountId;

    @Pattern(regexp = "[A-Z0-9._-]{1,32}")
    private String seccode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant to;

    @NotNull
    @Min(0)
    @Schema(defaultValue = "0")
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Max(100)
    @Schema(defaultValue = "20")
    private Integer size = 20;

    @AssertTrue(message = "Начало периода должно предшествовать концу")
    @Schema(hidden = true)
    public boolean isPeriodValid() {
        return from == null || to == null || from.isBefore(to);
    }

    public PageRequest pageRequest(String timestampField) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, timestampField, "id"));
    }
}
