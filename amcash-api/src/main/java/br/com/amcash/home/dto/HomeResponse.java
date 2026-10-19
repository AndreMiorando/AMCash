package br.com.amcash.home.dto;

import br.com.amcash.goal.entity.CustomGoalPeriod;
import br.com.amcash.goal.entity.PrimaryGoalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HomeResponse(
        LocalDate date,
        boolean hasFinancialEntries,
        FinancialSummary financial,
        GoalSummary goal,
        NextExpense nextExpense) {

    public record FinancialSummary(BigDecimal income, BigDecimal pendingExpenses,
                                   long pendingExpenseCount, BigDecimal balance) {
    }

    public record GoalSummary(PrimaryGoalType type, String name, CustomGoalPeriod period,
                              BigDecimal dailyTarget, BigDecimal periodRemaining,
                              boolean covered, boolean todaySelected) {
    }

    public record NextExpense(UUID id, String name, LocalDate dueDate, BigDecimal amount) {
    }
}
