package br.com.amcash.work.dto.response;

import br.com.amcash.work.entity.WorkCostCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record WorkCostResponse(
        UUID id,
        LocalDate date,
        WorkCostCategory category,
        String description,
        BigDecimal amount) {
}
