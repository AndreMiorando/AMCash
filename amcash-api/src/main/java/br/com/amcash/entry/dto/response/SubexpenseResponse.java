package br.com.amcash.entry.dto.response;

import br.com.amcash.entry.entity.RecurrenceFrequency;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubexpenseResponse(
        UUID id,
        String name,
        BigDecimal amount,
        String installmentDescription,
        boolean paid,
        RecurrenceFrequency recurrenceFrequency,
        int recurrenceCount,
        int recurrenceIndex,
        UUID seriesId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
