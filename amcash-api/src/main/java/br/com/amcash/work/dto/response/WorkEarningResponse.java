package br.com.amcash.work.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkEarningResponse(UUID id, String source, BigDecimal amount) {
}
