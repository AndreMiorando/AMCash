package br.com.amcash.work.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpsertWorkDayRequest(
        @NotNull(message = "Informe os quilômetros rodados")
        @DecimalMin(value = "0.00", message = "Os quilômetros não podem ser negativos")
        BigDecimal kilometers,

        UUID vehicleId,

        @NotNull(message = "Informe os faturamentos")
        @Size(max = 20, message = "Informe no máximo 20 fontes de faturamento")
        List<@Valid WorkEarningRequest> earnings) {
}
