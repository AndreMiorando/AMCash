package br.com.amcash.goal.dto.response;

import br.com.amcash.goal.entity.CustomGoalPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CustomGoalResponse(
        UUID id,
        String name,
        BigDecimal amount,
        CustomGoalPeriod period,
        LocalDate referenceDate,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal grossEarnings,
        BigDecimal costs,
        BigDecimal netResult,
        BigDecimal remainingAmount,
        BigDecimal dailyTarget,
        BigDecimal todayTarget,
        BigDecimal todayRemaining,
        int availableDays,
        BigDecimal progressPercentage,
        boolean achieved,
        boolean todaySelected,
        List<Integer> selectedDays) {
}
