package br.com.amcash.vehicle.dto.response;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record OdometerReadingResponse(UUID id,BigDecimal mileage,LocalDate readingDate,boolean correction){}
