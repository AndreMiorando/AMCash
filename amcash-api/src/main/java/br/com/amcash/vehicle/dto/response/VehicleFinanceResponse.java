package br.com.amcash.vehicle.dto.response;
import br.com.amcash.vehicle.entity.FinancialStatus; import java.math.BigDecimal; import java.time.LocalDate;
public record VehicleFinanceResponse(FinancialStatus status,BigDecimal monthlyAmount,Integer totalInstallments,Integer paidInstallments,
 LocalDate nextDueDate,Integer remainingInstallments,BigDecimal projectedRemainingInstallments,LocalDate projectedEndDate){}
