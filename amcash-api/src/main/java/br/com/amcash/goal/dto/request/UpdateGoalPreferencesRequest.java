package br.com.amcash.goal.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.time.DayOfWeek;
import java.util.Set;

public record UpdateGoalPreferencesRequest(
        @NotEmpty(message = "Selecione ao menos um dia disponível")
        Set<DayOfWeek> availableWeekdays
) {
}
