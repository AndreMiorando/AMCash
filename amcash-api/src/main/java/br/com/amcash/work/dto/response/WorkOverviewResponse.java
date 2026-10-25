package br.com.amcash.work.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WorkOverviewResponse(
        String period,
        LocalDate referenceDate,
        LocalDate rangeStart,
        LocalDate rangeEnd,
        WorkSummaryResponse summary,
        WorkEstimatedSummaryResponse estimatedSummary,
        WorkComparisonResponse comparison,
        List<WorkDayResponse> days,
        List<WorkCostResponse> costs) {
}
