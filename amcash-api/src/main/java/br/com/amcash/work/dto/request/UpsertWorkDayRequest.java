package br.com.amcash.work.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpsertWorkDayRequest(
        @NotNull(message = "Informe os quilômetros rodados")
        @DecimalMin(value = "0.00", message = "Os quilômetros não podem ser negativos")
        BigDecimal kilometers,

        @NotNull(message = "Informe o custo com combustível")
        @DecimalMin(value = "0.00", message = "O custo com combustível não pode ser negativo")
        BigDecimal fuelCost,

        @NotNull(message = "Informe os outros custos")
        @DecimalMin(value = "0.00", message = "Os outros custos não podem ser negativos")
        BigDecimal otherCosts,

        @NotNull(message = "Informe os faturamentos")
        @Size(max = 20, message = "Informe no máximo 20 fontes de faturamento")
        List<@Valid WorkEarningRequest> earnings) {
}
