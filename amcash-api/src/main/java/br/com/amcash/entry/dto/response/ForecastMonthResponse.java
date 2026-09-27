package br.com.amcash.entry.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ForecastMonthResponse(
        int year,
        int month,
        BigDecimal income,
        BigDecimal totalExpenses,
        BigDecimal paidExpenses,
        BigDecimal pendingExpenses,
        BigDecimal projectedBalance,
        BigDecimal freeBalance,
        BigDecimal expenseChange,
        BigDecimal freeBalanceChange,
        List<ForecastEventResponse> events
) {
}
