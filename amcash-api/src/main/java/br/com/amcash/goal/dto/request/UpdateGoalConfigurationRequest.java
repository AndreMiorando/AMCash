package br.com.amcash.goal.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateGoalConfigurationRequest(
        boolean includeExpenses,
        @NotNull(message = "Informe o valor adicional")
        @DecimalMin(value = "0.00", message = "O valor adicional não pode ser negativo") BigDecimal additionalAmount) {
}
