package br.com.amcash.goal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GoalResponse(
        int year,
        int month,
        LocalDate referenceDate,
        BigDecimal income,
        BigDecimal totalExpenses,
        BigDecimal paidExpenses,
        BigDecimal pendingExpenses,
        boolean includeExpenses,
        BigDecimal consideredExpenses,
        BigDecimal additionalAmount,
        BigDecimal targetAmount,
        BigDecimal realizedAmount,
        BigDecimal remainingAmount,
        BigDecimal weeklyTarget,
        BigDecimal dailyTarget,
        BigDecimal todayTarget,
        BigDecimal todayRemaining,
        int availableDays,
        BigDecimal progressPercentage,
        boolean covered,
        boolean todaySelected,
        List<Integer> selectedDays
) {
}
