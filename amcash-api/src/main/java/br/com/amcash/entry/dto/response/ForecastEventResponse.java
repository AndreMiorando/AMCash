package br.com.amcash.entry.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ForecastEventResponse(
        String type,
        String source,
        String name,
        BigDecimal amount,
        LocalDate date,
        String recurrenceFrequency
) {
}
