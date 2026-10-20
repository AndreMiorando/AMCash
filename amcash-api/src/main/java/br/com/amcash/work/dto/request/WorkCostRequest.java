package br.com.amcash.work.dto.request;

import br.com.amcash.work.entity.WorkCostCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkCostRequest(
        @NotNull(message = "Informe a data do custo") LocalDate date,
        @NotNull(message = "Informe o tipo do custo") WorkCostCategory category,
        @Size(max = 60, message = "A descrição deve ter no máximo 60 caracteres") String description,
        @NotNull(message = "Informe o valor do custo")
        @DecimalMin(value = "0.01", message = "O custo deve ser maior que zero") BigDecimal amount) {
}
