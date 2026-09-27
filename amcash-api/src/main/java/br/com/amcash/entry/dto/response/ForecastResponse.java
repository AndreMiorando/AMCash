package br.com.amcash.entry.dto.response;

import java.util.List;

public record ForecastResponse(
        int startYear,
        int startMonth,
        int months,
        List<ForecastMonthResponse> timeline
) {
}
