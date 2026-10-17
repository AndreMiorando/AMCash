package br.com.amcash.work.dto.response;

import java.math.BigDecimal;

public record WorkSummaryResponse(
        BigDecimal grossEarnings,
        BigDecimal fuelCost,
        BigDecimal otherCosts,
        BigDecimal totalCosts,
        BigDecimal netEarnings,
        BigDecimal kilometers,
        BigDecimal earningsPerKilometer,
        int workedDays) {
}
