package br.com.amcash.entry.dto.response;

import java.util.List;

public record MonthlyEntriesResponse(
        int year,
        int month,
        MonthlySummaryResponse summary,
        List<EntryResponse> entries
) {
}
