package br.com.amcash.work.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WorkEarningRequest(
        @NotBlank(message = "Informe a fonte do faturamento")
        @Size(max = 30, message = "A fonte deve ter no máximo 30 caracteres")
        String source,

        @NotNull(message = "Informe o valor faturado")
        @DecimalMin(value = "0.00", message = "O faturamento não pode ser negativo")
        BigDecimal amount) {
}
