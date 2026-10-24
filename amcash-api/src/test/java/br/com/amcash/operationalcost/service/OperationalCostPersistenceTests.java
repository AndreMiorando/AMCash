package br.com.amcash.operationalcost.service;

import br.com.amcash.operationalcost.dto.request.OperationalCostRequest;
import br.com.amcash.operationalcost.entity.*;
import br.com.amcash.shared.exception.ConflictException;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.*;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.vehicle.dto.request.VehicleRequest;
import br.com.amcash.vehicle.entity.*;
import br.com.amcash.vehicle.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OperationalCostPersistenceTests {
    @Autowired private OperationalCostService operationalCosts; @Autowired private VehicleService vehicles; @Autowired private UserRepository users;

    @Test void shouldPersistOneIndependentConfigurationPerVehicle(){User user=new User("operational-persistence","operational@amcash.test","Motorista",null);user.updateUsageProfile(UsageProfile.DELIVERY);User savedUser=users.save(user);var vehicle=vehicles.create(savedUser.getId(),new VehicleRequest(VehicleType.MOTORCYCLE,"Honda","CG",2024,2024,null,"Moto",null,FuelType.GASOLINE,new BigDecimal("12000"),LocalDate.of(2026,10,24),new BigDecimal("35"),null,null,null,FinancialStatus.PAID_OFF,null,null,null,null,null));var request=new OperationalCostRequest(new BigDecimal("2400"),FuelType.GASOLINE,new BigDecimal("6"),false,BigDecimal.ZERO,new BigDecimal("180"),null,null,MaintenanceCalculationMethod.MANUAL,new BigDecimal("1200"),false,null,null,List.of(new OperationalCostRequest.CustomFixedCostRequest("Lavagem",new BigDecimal("60"),CostPeriod.MONTHLY)),Set.of());operationalCosts.create(savedUser.getId(),vehicle.id(),request);var persisted=operationalCosts.get(savedUser.getId(),vehicle.id());assertEquals("Moto",persisted.vehicleName());assertEquals(new BigDecimal("586.43"),persisted.calculation().totalMonthly());assertThrows(ConflictException.class,()->operationalCosts.create(savedUser.getId(),vehicle.id(),request));operationalCosts.delete(savedUser.getId(),vehicle.id());assertThrows(NotFoundException.class,()->operationalCosts.get(savedUser.getId(),vehicle.id()));}
}
