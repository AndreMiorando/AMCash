package br.com.amcash.entry.dto.request;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.EntryCategory;
import br.com.amcash.entry.entity.RecurrenceFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateEntryRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 160, message = "A descrição deve ter no máximo 160 caracteres")
        String name,

        @NotNull(message = "A categoria é obrigatória")
        EntryCategory category,

        @NotNull(message = "O tipo é obrigatório")
        EntryType type,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "O valor deve ter no máximo duas casas decimais")
        BigDecimal amount,

        @NotNull(message = "A data é obrigatória")
        LocalDate dueDate,

        @NotNull(message = "A frequência de repetição é obrigatória")
        RecurrenceFrequency recurrenceFrequency,

        @Min(value = 0, message = "A quantidade de repetições não pode ser negativa")
        @Max(value = 120, message = "A quantidade máxima é 120 repetições")
        int recurrenceCount,

        boolean hasSubexpenses
) {
}
