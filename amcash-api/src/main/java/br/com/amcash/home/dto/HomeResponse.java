package br.com.amcash.home.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HomeResponse(
        LocalDate date,
        FinancialSummary financial,
        WorkSummary todayWork,
        WorkSummary monthWork,
        GoalSummary goal) {

    public record FinancialSummary(BigDecimal income, BigDecimal totalExpenses,
                                   BigDecimal paidExpenses, BigDecimal pendingExpenses) {
    }

    public record WorkSummary(BigDecimal gross, BigDecimal costs, BigDecimal net) {
    }

    public record GoalSummary(BigDecimal monthRemaining, BigDecimal todayTarget,
                              BigDecimal todayAchieved, BigDecimal todayRemaining,
                              BigDecimal todaySurplus, BigDecimal progressPercentage,
                              int availableDays, boolean covered, boolean todaySelected) {
    }
}
