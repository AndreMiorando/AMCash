package br.com.amcash.goal.dto.response;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record GoalResponse(
        int year,
        int month,
        LocalDate referenceDate,
        BigDecimal income,
        BigDecimal pendingExpenses,
        BigDecimal remainingAmount,
        BigDecimal weeklyTarget,
        BigDecimal dailyTarget,
        int availableDays,
        BigDecimal progressPercentage,
        boolean covered,
        List<DayOfWeek> availableWeekdays
) {
}
