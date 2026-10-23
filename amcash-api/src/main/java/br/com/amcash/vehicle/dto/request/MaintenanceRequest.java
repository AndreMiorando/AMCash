package br.com.amcash.vehicle.dto.request;
import br.com.amcash.vehicle.entity.MaintenanceType; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate;
public record MaintenanceRequest(@NotNull MaintenanceType type,@Size(max=80) String customName,@NotNull LocalDate date,
 @NotNull @DecimalMin("0.0") BigDecimal mileage,@NotNull @DecimalMin("0.0") BigDecimal amount,@Size(max=300) String description,
 @Positive BigDecimal nextMileage,LocalDate nextDate){}
