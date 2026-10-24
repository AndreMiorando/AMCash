package br.com.amcash.operationalcost.dto.request;

import br.com.amcash.operationalcost.entity.MileageSource;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OperationalMileageUpdateRequest(@NotNull @Positive BigDecimal monthlyMileage,@NotNull MileageSource mileageSource) {}
