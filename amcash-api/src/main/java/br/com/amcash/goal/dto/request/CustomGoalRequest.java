package br.com.amcash.goal.dto.request;

import br.com.amcash.goal.entity.CustomGoalPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustomGoalRequest(
        @NotBlank(message = "Informe o nome da meta")
        @Size(max = 40, message = "O nome deve ter no máximo 40 caracteres") String name,
        @NotNull(message = "Informe o valor da meta")
        @DecimalMin(value = "0.01", message = "O valor da meta deve ser maior que zero") BigDecimal amount,
        @NotNull(message = "Informe a periodicidade") CustomGoalPeriod period) {
}
