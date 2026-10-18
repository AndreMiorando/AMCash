package br.com.amcash.goal.dto.request;

import br.com.amcash.goal.entity.PrimaryGoalType;
import jakarta.validation.constraints.NotNull;

public record UpdatePrimaryGoalRequest(@NotNull PrimaryGoalType type) {
}
