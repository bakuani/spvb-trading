package ru.spvb.trading.domain.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "instruments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instrument {
    @Id
    @Column(length = 32)
    private String seccode;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false)
    private long lotSize;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal minStep;

    @Column(nullable = false, length = 3)
    private String currency;
}
