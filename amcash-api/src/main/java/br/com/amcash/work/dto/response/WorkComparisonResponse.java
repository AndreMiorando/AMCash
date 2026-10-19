package br.com.amcash.work.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkComparisonResponse(
        LocalDate rangeStart,
        LocalDate rangeEnd,
        BigDecimal netEarnings,
        BigDecimal changeAmount,
        BigDecimal changePercentage) {
}
