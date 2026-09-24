package br.com.amcash.entry.dto.response;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.EntryCategory;
import br.com.amcash.entry.entity.RecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EntryResponse(
        UUID id,
        String name,
        EntryCategory category,
        EntryType type,
        BigDecimal amount,
        LocalDate dueDate,
        RecurrenceFrequency recurrenceFrequency,
        int recurrenceCount,
        int recurrenceIndex,
        UUID seriesId,
        boolean hasSubexpenses,
        boolean paid,
        int paidOccurrences,
        int totalOccurrences,
        int completedSubexpenses,
        int totalSubexpenses,
        List<SubexpenseResponse> subexpenses,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
