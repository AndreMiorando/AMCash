package br.com.amcash.vehicle.dto.request;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate;
public record OdometerRequest(@NotNull @DecimalMin("0.0") BigDecimal mileage,@NotNull LocalDate readingDate,boolean correction){}
