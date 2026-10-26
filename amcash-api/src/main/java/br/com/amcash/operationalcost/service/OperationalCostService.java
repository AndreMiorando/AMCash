package br.com.amcash.operationalcost.service;

import br.com.amcash.operationalcost.dto.request.OperationalCostRequest;
import br.com.amcash.operationalcost.dto.request.OperationalMileageUpdateRequest;
import br.com.amcash.operationalcost.dto.response.OperationalCostResponse;
import br.com.amcash.operationalcost.entity.*;
import br.com.amcash.operationalcost.repository.OperationalCostConfigurationRepository;
import br.com.amcash.shared.exception.*;
import br.com.amcash.user.entity.*;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.vehicle.entity.*;
import br.com.amcash.vehicle.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class OperationalCostService {
    private static final BigDecimal TWELVE=BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED=BigDecimal.valueOf(100);
    private static final int MIN_HISTORY_DAYS=90;
    private final OperationalCostConfigurationRepository configurations;
    private final VehicleRepository vehicles;
    private final VehicleFinanceRepository finances;
    private final VehicleMaintenanceRepository maintenances;
    private final OdometerReadingRepository readings;
    private final UserRepository users;

    public OperationalCostService(OperationalCostConfigurationRepository configurations,VehicleRepository vehicles,VehicleFinanceRepository finances,VehicleMaintenanceRepository maintenances,OdometerReadingRepository readings,UserRepository users){this.configurations=configurations;this.vehicles=vehicles;this.finances=finances;this.maintenances=maintenances;this.readings=readings;this.users=users;}

    @Transactional(readOnly=true) public List<OperationalCostResponse> list(UUID userId){requireMobility(userId);return configurations.findAllByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::response).toList();}
    @Transactional(readOnly=true) public OperationalCostResponse get(UUID userId,UUID vehicleId){requireMobility(userId);return response(findConfiguration(userId,vehicleId));}
    @Transactional public OperationalCostResponse create(UUID userId,UUID vehicleId,OperationalCostRequest request){User user=requireMobility(userId);Vehicle vehicle=findVehicle(userId,vehicleId);if(configurations.existsByVehicleId(vehicleId))throw new ConflictException("Este veículo já possui uma configuração de custo operacional");if(request.mileageSource()!=MileageSource.MANUAL)throw new BadRequestException("Informe manualmente a quilometragem no primeiro cálculo");OperationalCostConfiguration configuration=new OperationalCostConfiguration(user,vehicle);apply(configuration,vehicle,request);return response(configurations.save(configuration));}
    @Transactional public OperationalCostResponse update(UUID userId,UUID vehicleId,OperationalCostRequest request){requireMobility(userId);Vehicle vehicle=findVehicle(userId,vehicleId);OperationalCostConfiguration configuration=findConfiguration(userId,vehicleId);apply(configuration,vehicle,request);return response(configuration);}
    @Transactional public OperationalCostResponse updateMileage(UUID userId,UUID vehicleId,OperationalMileageUpdateRequest request){requireMobility(userId);OperationalCostConfiguration configuration=findConfiguration(userId,vehicleId);configuration.updateMileage(request.monthlyMileage(),request.mileageSource());return response(configuration);}
    @Transactional public void delete(UUID userId,UUID vehicleId){requireMobility(userId);configurations.delete(findConfiguration(userId,vehicleId));}

    private void apply(OperationalCostConfiguration configuration,Vehicle vehicle,OperationalCostRequest request){
        validate(vehicle,request);
        List<OperationalCustomFixedCost> custom=request.customFixedCosts().stream().map(item->new OperationalCustomFixedCost(item.title().trim(),item.amount(),item.period(),item.workCostCategory())).toList();
        configuration.update(request.monthlyMileage(),request.mileageSource(),request.fuelType(),request.fuelPrice(),request.includeFinance(),request.annualIpva(),request.annualFees(),request.insuranceAmount(),request.insurancePeriod(),request.maintenanceMethod(),request.maintenanceMethod()==MaintenanceCalculationMethod.MANUAL?request.annualMaintenanceEstimate():null,request.includeDepreciation(),request.includeDepreciation()?request.currentVehicleValue():null,request.includeDepreciation()?request.annualDepreciationRate():null,custom);
    }

    private void validate(Vehicle vehicle,OperationalCostRequest request){
        BigDecimal consumption=consumption(vehicle,request.fuelType());
        if(consumption==null||consumption.signum()<=0)throw new BadRequestException("Complete o consumo deste combustível no cadastro do veículo");
        if(request.insuranceAmount()!=null&&request.insurancePeriod()==null)throw new BadRequestException("Informe a periodicidade do seguro");
        if(request.insuranceAmount()==null&&request.insurancePeriod()!=null)throw new BadRequestException("Informe o valor do seguro");
        if(request.includeFinance()&&(vehicleFinance(vehicle).getStatus()==FinancialStatus.PAID_OFF||vehicleFinance(vehicle).getMonthlyAmount()==null))throw new BadRequestException("O veículo não possui financiamento ou aluguel mensal disponível");
        if(request.maintenanceMethod()==MaintenanceCalculationMethod.MANUAL&&request.annualMaintenanceEstimate()==null)throw new BadRequestException("Informe a estimativa anual de manutenção");
        if(request.maintenanceMethod()==MaintenanceCalculationMethod.LAST_12_MONTHS){
            OperationalCostResponse.MaintenanceHistoryResponse history=maintenanceHistory(vehicle);
            if(!history.available())throw new BadRequestException(history.unavailableReason());
        }
        if(request.includeDepreciation()&&(request.currentVehicleValue()==null||request.annualDepreciationRate()==null))throw new BadRequestException("Informe o valor atual e a taxa anual de depreciação");
    }

    private OperationalCostResponse response(OperationalCostConfiguration configuration){
        Vehicle vehicle=configuration.getVehicle();
        var calculation=calculation(configuration);
        List<OperationalCostResponse.CustomFixedCostResponse> custom=configuration.getCustomFixedCosts().stream().map(item->new OperationalCostResponse.CustomFixedCostResponse(item.getTitle(),item.getAmount(),item.getPeriod(),item.getWorkCostCategory())).toList();
        String name=vehicle.getNickname()!=null?vehicle.getNickname():vehicle.getBrand()+" "+vehicle.getModel();
        return new OperationalCostResponse(configuration.getId(),vehicle.getId(),name,configuration.getFuelType(),configuration.getMonthlyMileage(),configuration.getMileageSource(),configuration.getFuelPrice(),configuration.isIncludeFinance(),configuration.getAnnualIpva(),configuration.getAnnualFees(),configuration.getInsuranceAmount(),configuration.getInsurancePeriod(),configuration.getMaintenanceMethod(),configuration.getAnnualMaintenanceEstimate(),configuration.isIncludeDepreciation(),configuration.getCurrentVehicleValue(),configuration.getAnnualDepreciationRate(),custom,configuration.getMaintenanceMethod()==MaintenanceCalculationMethod.LAST_12_MONTHS?maintenanceHistory(vehicle):null,calculation);
    }

    @Transactional(readOnly=true)
    public OperationalCostResponse.CalculationResponse calculation(OperationalCostConfiguration configuration){
        Vehicle vehicle=configuration.getVehicle();
        BigDecimal mileage=configuration.getMonthlyMileage();
        BigDecimal fuelPerKm=configuration.getFuelType()==FuelType.ELECTRIC?consumption(vehicle,FuelType.ELECTRIC).multiply(configuration.getFuelPrice()).divide(HUNDRED,10,RoundingMode.HALF_UP):configuration.getFuelPrice().divide(consumption(vehicle,configuration.getFuelType()),10,RoundingMode.HALF_UP);
        BigDecimal fuelMonthly=fuelPerKm.multiply(mileage);
        VehicleFinance finance=vehicleFinance(vehicle);
        BigDecimal financeMonthly=configuration.isIncludeFinance()&&finance.getMonthlyAmount()!=null?finance.getMonthlyAmount():BigDecimal.ZERO;
        BigDecimal taxesMonthly=configuration.getAnnualIpva().add(configuration.getAnnualFees()).divide(TWELVE,10,RoundingMode.HALF_UP);
        BigDecimal insuranceMonthly=configuration.getInsuranceAmount()==null?BigDecimal.ZERO:monthly(configuration.getInsuranceAmount(),configuration.getInsurancePeriod());
        BigDecimal otherMonthly=configuration.getCustomFixedCosts().stream().map(item->monthly(item.getAmount(),item.getPeriod())).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal maintenanceMonthly=maintenanceMonthly(configuration);
        BigDecimal total=fuelMonthly.add(financeMonthly).add(taxesMonthly).add(insuranceMonthly).add(otherMonthly).add(maintenanceMonthly);
        BigDecimal depreciationAnnual=configuration.isIncludeDepreciation()?configuration.getCurrentVehicleValue().multiply(configuration.getAnnualDepreciationRate()).divide(HUNDRED,10,RoundingMode.HALF_UP):BigDecimal.ZERO;
        BigDecimal depreciationMonthly=depreciationAnnual.divide(TWELVE,10,RoundingMode.HALF_UP);
        return new OperationalCostResponse.CalculationResponse(money(fuelMonthly),money(financeMonthly),money(taxesMonthly),money(insuranceMonthly),money(otherMonthly),money(maintenanceMonthly),money(total),perKm(total,mileage),money(depreciationAnnual),money(depreciationMonthly),perKm(depreciationMonthly,mileage));
    }

    private BigDecimal maintenanceMonthly(OperationalCostConfiguration configuration){
        if(configuration.getMaintenanceMethod()==MaintenanceCalculationMethod.MANUAL)return configuration.getAnnualMaintenanceEstimate().divide(TWELVE,10,RoundingMode.HALF_UP);
        OperationalCostResponse.MaintenanceHistoryResponse history=maintenanceHistory(configuration.getVehicle());
        if(!history.available())return BigDecimal.ZERO;
        return history.amount().divide(history.kilometers(),10,RoundingMode.HALF_UP).multiply(configuration.getMonthlyMileage());
    }

    @Transactional(readOnly=true)
    public OperationalCostResponse.MaintenanceHistoryResponse maintenanceHistory(Vehicle vehicle){
        LocalDate end=LocalDate.now();
        List<OdometerReading> history=readings.findAllByVehicleIdOrderByReadingDateDescCreatedAtDesc(vehicle.getId());
        if(history.size()<2)return unavailableHistory("Registre ao menos duas leituras de quilometragem para usar o histórico do último ano.");
        LocalDate windowStart=end.minusMonths(12);
        OdometerReading latest=history.get(0);
        OdometerReading start=history.stream().filter(item->!item.getReadingDate().isAfter(windowStart)).findFirst().orElse(history.get(history.size()-1));
        if(start==latest||ChronoUnit.DAYS.between(start.getReadingDate(),end)<MIN_HISTORY_DAYS)return unavailableHistory("Histórico insuficiente: são necessários ao menos 90 dias de leituras. Use a estimativa manual ou registre mais leituras.");
        BigDecimal kilometers=latest.getMileage().subtract(start.getMileage());
        if(kilometers.signum()<=0)return unavailableHistory("As leituras de quilometragem não permitem calcular o histórico do último ano.");
        List<VehicleMaintenance> items=maintenances.findAllByVehicleIdOrderByDateDescCreatedAtDesc(vehicle.getId()).stream().filter(item->item.getDate().isAfter(start.getReadingDate())&&!item.getDate().isAfter(end)).toList();
        BigDecimal amount=items.stream().map(VehicleMaintenance::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new OperationalCostResponse.MaintenanceHistoryResponse(true,null,start.getReadingDate(),end,kilometers,money(amount),items.size(),perKm(amount,kilometers));
    }

    private OperationalCostResponse.MaintenanceHistoryResponse unavailableHistory(String reason){return new OperationalCostResponse.MaintenanceHistoryResponse(false,reason,null,null,null,null,0,null);}
    private BigDecimal consumption(Vehicle vehicle,FuelType fuel){return switch(fuel){case GASOLINE->allowed(vehicle,fuel)?vehicle.getGasolineConsumption():null;case ETHANOL->allowed(vehicle,fuel)?vehicle.getEthanolConsumption():null;case DIESEL->allowed(vehicle,fuel)?vehicle.getDieselConsumption():null;case ELECTRIC->allowed(vehicle,fuel)?vehicle.getElectricConsumption():null;default->null;};}
    private boolean allowed(Vehicle vehicle,FuelType selected){return switch(vehicle.getFuelType()){case FLEX->selected==FuelType.GASOLINE||selected==FuelType.ETHANOL;case HYBRID->selected==FuelType.GASOLINE||selected==FuelType.ETHANOL||selected==FuelType.DIESEL||selected==FuelType.ELECTRIC;default->vehicle.getFuelType()==selected;};}
    private BigDecimal monthly(BigDecimal value,CostPeriod period){return period==CostPeriod.ANNUAL?value.divide(TWELVE,10,RoundingMode.HALF_UP):value;}
    private BigDecimal money(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP);}
    private BigDecimal perKm(BigDecimal value,BigDecimal mileage){return value.divide(mileage,4,RoundingMode.HALF_UP);}
    private VehicleFinance vehicleFinance(Vehicle vehicle){return finances.findById(vehicle.getId()).orElseThrow(()->new BadRequestException("Complete a situação financeira do veículo"));}
    private User requireMobility(UUID userId){User user=users.findById(userId).orElseThrow(()->new NotFoundException("Usuário não encontrado"));if(user.getUsageProfile()==null||user.getUsageProfile()==UsageProfile.PERSONAL)throw new ForbiddenException("Custo operacional está disponível apenas para motoristas e entregadores");return user;}
    private Vehicle findVehicle(UUID userId,UUID vehicleId){return vehicles.findByIdAndUserId(vehicleId,userId).orElseThrow(()->new NotFoundException("Veículo não encontrado"));}
    private OperationalCostConfiguration findConfiguration(UUID userId,UUID vehicleId){return configurations.findByVehicleIdAndUserId(vehicleId,userId).orElseThrow(()->new NotFoundException("Configuração de custo operacional não encontrada"));}
}
