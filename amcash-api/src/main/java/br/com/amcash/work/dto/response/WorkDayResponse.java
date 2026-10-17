package br.com.amcash.work.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WorkDayResponse(
        UUID id,
        LocalDate date,
        BigDecimal kilometers,
        BigDecimal grossEarnings,
        BigDecimal earningsPerKilometer,
        List<WorkEarningResponse> earnings) {
}
