package br.com.amcash.entry.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubexpenseResponse(
        UUID id,
        String name,
        BigDecimal amount,
        String installmentDescription,
        boolean paid,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
