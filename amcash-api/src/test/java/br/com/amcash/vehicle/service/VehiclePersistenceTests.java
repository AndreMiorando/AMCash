package br.com.amcash.vehicle.service;

import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.UsageProfile;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.vehicle.dto.request.VehicleRequest;
import br.com.amcash.vehicle.entity.FinancialStatus;
import br.com.amcash.vehicle.entity.FuelType;
import br.com.amcash.vehicle.entity.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class VehiclePersistenceTests {
    @Autowired private VehicleService service;
    @Autowired private UserRepository users;

    @Test
    void shouldPersistACompleteVehicle() {
        User user = new User("vehicle-persistence", "vehicle@amcash.test", "Motorista", null);
        user.updateUsageProfile(UsageProfile.RIDE_DRIVER);
        user = users.save(user);
        var userId = user.getId();

        var response = service.create(userId, new VehicleRequest(
                VehicleType.CAR, "Honda", "Fit", 2015, 2016, "EX", "Meu Fit", "ABC1D23",
                FuelType.FLEX, new BigDecimal("85420"), LocalDate.of(2026, 10, 23),
                new BigDecimal("11"), new BigDecimal("8"), null, null, FinancialStatus.FINANCED,
                new BigDecimal("1330"), 60, 22, LocalDate.of(2026, 11, 10),
                LocalDate.of(2024, 5, 10)));

        assertEquals("Honda", response.brand());
        assertEquals(38, response.finance().remainingInstallments());
    }

    @Test
    void shouldDeleteVehicleAndItsRelatedData() {
        User user = new User("vehicle-delete", "vehicle-delete@amcash.test", "Motorista", null);
        user.updateUsageProfile(UsageProfile.RIDE_DRIVER);
        user = users.save(user);
        var userId = user.getId();

        var response = service.create(userId, new VehicleRequest(
                VehicleType.CAR, "Honda", "Fit", 2015, 2016, null, null, null,
                FuelType.FLEX, new BigDecimal("1000"), LocalDate.of(2026, 10, 23),
                new BigDecimal("11"), new BigDecimal("8"), null, null, FinancialStatus.PAID_OFF,
                null, null, null, null, null));

        service.delete(userId, response.id());

        assertThrows(NotFoundException.class, () -> service.get(userId, response.id()));
    }
}
