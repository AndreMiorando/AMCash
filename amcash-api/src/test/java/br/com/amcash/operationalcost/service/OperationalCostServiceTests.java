package br.com.amcash.operationalcost.service;

import br.com.amcash.operationalcost.dto.request.OperationalCostRequest;
import br.com.amcash.operationalcost.entity.*;
import br.com.amcash.operationalcost.repository.OperationalCostConfigurationRepository;
import br.com.amcash.user.entity.*;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.vehicle.entity.*;
import br.com.amcash.vehicle.repository.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OperationalCostServiceTests {
    private final OperationalCostConfigurationRepository configurations=mock(OperationalCostConfigurationRepository.class);
    private final VehicleRepository vehicles=mock(VehicleRepository.class); private final VehicleFinanceRepository finances=mock(VehicleFinanceRepository.class);
    private final VehicleMaintenanceRepository maintenances=mock(VehicleMaintenanceRepository.class); private final UserRepository users=mock(UserRepository.class);
    private final OperationalCostService service=new OperationalCostService(configurations,vehicles,finances,maintenances,users);
    private User user; private Vehicle vehicle;

    @BeforeEach void setUp(){user=new User("operational","driver@example.com","Driver",null);user.setId(UUID.randomUUID());user.updateUsageProfile(UsageProfile.RIDE_DRIVER);vehicle=new Vehicle(user);vehicle.update(VehicleType.CAR,"Honda","Fit",2020,2020,null,"Meu carro",null,FuelType.GASOLINE,new BigDecimal("50000"),LocalDate.now(),new BigDecimal("12"),null,null,null,null);VehicleFinance finance=new VehicleFinance(vehicle);finance.update(FinancialStatus.FINANCED,new BigDecimal("1000"),48,12,LocalDate.now());when(users.findById(user.getId())).thenReturn(Optional.of(user));when(vehicles.findByIdAndUserId(vehicle.getId(),user.getId())).thenReturn(Optional.of(vehicle));when(finances.findById(vehicle.getId())).thenReturn(Optional.of(finance));when(configurations.save(any())).thenAnswer(invocation->invocation.getArgument(0));}

    @Test void shouldCalculateCashCostAndKeepDepreciationSeparate(){var request=new OperationalCostRequest(new BigDecimal("3000"),MileageSource.MANUAL,FuelType.GASOLINE,new BigDecimal("6"),true,new BigDecimal("1200"),new BigDecimal("600"),new BigDecimal("1200"),CostPeriod.ANNUAL,MaintenanceCalculationMethod.MANUAL,new BigDecimal("1200"),true,new BigDecimal("60000"),new BigDecimal("10"),List.of(new OperationalCostRequest.CustomFixedCostRequest("Garagem",new BigDecimal("50"),CostPeriod.MONTHLY),new OperationalCostRequest.CustomFixedCostRequest("Rastreamento",new BigDecimal("120"),CostPeriod.ANNUAL)),Set.of());var result=service.create(user.getId(),vehicle.getId(),request);assertEquals(new BigDecimal("2910.00"),result.calculation().totalMonthly());assertEquals(new BigDecimal("0.9700"),result.calculation().cashCostPerKm());assertEquals(new BigDecimal("500.00"),result.calculation().depreciationMonthly());assertEquals(new BigDecimal("0.1667"),result.calculation().depreciationPerKm());}

    @Test void shouldUseOnlyTheLatestRepeatedMaintenance(){VehicleMaintenance older=maintenance(new BigDecimal("200"),new BigDecimal("10000"),new BigDecimal("15000"),LocalDate.now().minusMonths(5));VehicleMaintenance latest=maintenance(new BigDecimal("300"),new BigDecimal("20000"),new BigDecimal("30000"),LocalDate.now());when(maintenances.findAllById(any())).thenReturn(List.of(older,latest));when(maintenances.findAllByVehicleIdOrderByDateDescCreatedAtDesc(vehicle.getId())).thenReturn(List.of(latest,older));var request=new OperationalCostRequest(new BigDecimal("3000"),MileageSource.MANUAL,FuelType.GASOLINE,new BigDecimal("6"),false,BigDecimal.ZERO,BigDecimal.ZERO,null,null,MaintenanceCalculationMethod.HISTORY,null,false,null,null,List.of(),Set.of(older.getId(),latest.getId()));var result=service.create(user.getId(),vehicle.getId(),request);assertEquals(new BigDecimal("90.00"),result.calculation().maintenanceMonthly());assertEquals(new BigDecimal("1590.00"),result.calculation().totalMonthly());}

    private VehicleMaintenance maintenance(BigDecimal amount,BigDecimal mileage,BigDecimal nextMileage,LocalDate date){VehicleMaintenance item=new VehicleMaintenance(vehicle);item.update(MaintenanceType.OIL_CHANGE,null,date,mileage,amount,null,nextMileage,null);return item;}
}
