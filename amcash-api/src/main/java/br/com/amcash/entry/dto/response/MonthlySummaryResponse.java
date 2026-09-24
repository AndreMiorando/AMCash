package br.com.amcash.entry.dto.response;

import java.math.BigDecimal;

public record MonthlySummaryResponse(
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance
) {
}
