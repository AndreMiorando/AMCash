package br.com.amcash.goal.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateGoalPreferencesRequest(
        @NotNull(message = "Informe os dias disponíveis")
        Set<Integer> selectedDays
) {
}
