package br.com.amcash.entry.dto.request;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.EntryCategory;
import br.com.amcash.entry.entity.RecurrenceFrequency;
import br.com.amcash.entry.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateEntryRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 40, message = "A descrição deve ter no máximo 40 caracteres")
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

        @Min(value = 0, message = "O total de parcelas não pode ser negativo")
        @Max(value = 120, message = "O máximo é 120 parcelas")
        int recurrenceCount,

        boolean hasSubexpenses,

        List<@Valid SubexpenseRequest> subexpenses,

        PaymentMethod paymentMethod,

        @Size(max = 120, message = "As observações devem ter no máximo 120 caracteres")
        String notes
) {
    public CreateEntryRequest(
            String name,
            EntryCategory category,
            EntryType type,
            BigDecimal amount,
            LocalDate dueDate,
            RecurrenceFrequency recurrenceFrequency,
            int recurrenceCount,
            boolean hasSubexpenses,
            List<SubexpenseRequest> subexpenses) {
        this(name, category, type, amount, dueDate, recurrenceFrequency, recurrenceCount,
                hasSubexpenses, subexpenses, null, null);
    }
}
