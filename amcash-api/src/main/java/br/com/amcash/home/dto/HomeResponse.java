package br.com.amcash.home.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HomeResponse(
        LocalDate date,
        FinancialSummary financial,
        GoalSummary goal,
        NextExpense nextExpense) {

    public record FinancialSummary(BigDecimal income, BigDecimal pendingExpenses, BigDecimal balance) {
    }

    public record GoalSummary(BigDecimal dailyTarget, boolean covered, boolean todaySelected) {
    }

    public record NextExpense(UUID id, String name, LocalDate dueDate, BigDecimal amount) {
    }
}
