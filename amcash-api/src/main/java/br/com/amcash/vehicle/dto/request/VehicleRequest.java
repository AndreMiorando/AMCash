package br.com.amcash.vehicle.dto.request;
import br.com.amcash.vehicle.entity.*; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate;
public record VehicleRequest(@NotNull VehicleType type,@NotBlank @Size(max=60) String brand,@NotBlank @Size(max=80) String model,
 @Min(1900) @Max(2200) int manufactureYear,@Min(1900) @Max(2201) int modelYear,@Size(max=80) String version,@Size(max=60) String nickname,
 @Pattern(regexp="^$|[A-Za-z]{3}(?:[0-9][A-Za-z][0-9]{2}|[0-9]{4})$") String licensePlate,@NotNull FuelType fuelType,
 @NotNull @DecimalMin("0.0") BigDecimal currentMileage,@NotNull LocalDate mileageReadingDate,
 @Positive BigDecimal gasolineConsumption,@Positive BigDecimal ethanolConsumption,@Positive BigDecimal electricConsumption,
 @NotNull FinancialStatus financialStatus,@Positive BigDecimal monthlyAmount,@Positive Integer totalInstallments,@PositiveOrZero Integer paidInstallments,
 LocalDate nextDueDate,LocalDate acquisitionDate){}
