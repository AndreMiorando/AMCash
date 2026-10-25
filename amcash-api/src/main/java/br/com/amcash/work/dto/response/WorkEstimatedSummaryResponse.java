package br.com.amcash.work.dto.response;

import java.math.BigDecimal;

public record WorkEstimatedSummaryResponse(
        boolean available,
        String unavailableReason,
        BigDecimal grossEarnings,
        BigDecimal operationalCost,
        BigDecimal additionalCosts,
        BigDecimal totalCosts,
        BigDecimal netEarnings,
        BigDecimal kilometers,
        BigDecimal earningsPerKilometer,
        BigDecimal resultPerKilometer) {
}
