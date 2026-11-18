package ru.spvb.trading.api.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, int page, int size) {
    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(source.getContent(), source.getTotalElements(), source.getNumber(), source.getSize());
    }
}
