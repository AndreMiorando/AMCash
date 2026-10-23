package br.com.amcash.vehicle.dto.response;
import br.com.amcash.vehicle.entity.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.*;
public record VehicleResponse(UUID id,VehicleType type,String brand,String model,int manufactureYear,int modelYear,String version,String nickname,
 String licensePlate,FuelType fuelType,BigDecimal currentMileage,LocalDate mileageReadingDate,BigDecimal gasolineConsumption,
 BigDecimal ethanolConsumption,BigDecimal electricConsumption,LocalDate acquisitionDate,boolean hasPhoto,VehicleFinanceResponse finance,
 List<OdometerReadingResponse> odometerReadings,List<MaintenanceResponse> maintenances){}
