package br.com.amcash.vehicle.dto.response;
import br.com.amcash.vehicle.entity.MaintenanceType; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record MaintenanceResponse(UUID id,MaintenanceType type,String customName,LocalDate date,BigDecimal mileage,BigDecimal amount,String description,BigDecimal nextMileage,LocalDate nextDate){}
