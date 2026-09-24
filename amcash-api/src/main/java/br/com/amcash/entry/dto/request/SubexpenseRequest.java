package br.com.amcash.entry.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SubexpenseRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 160, message = "A descrição deve ter no máximo 160 caracteres")
        String name,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "O valor deve ter no máximo duas casas decimais")
        BigDecimal amount,

        @Size(max = 80, message = "A descrição da parcela deve ter no máximo 80 caracteres")
        String installmentDescription,

        boolean paid
) {
}
