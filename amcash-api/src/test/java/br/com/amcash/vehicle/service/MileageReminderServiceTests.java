package br.com.amcash.vehicle.service;

import br.com.amcash.user.entity.User;
import br.com.amcash.vehicle.entity.*;
import br.com.amcash.vehicle.repository.VehicleMileageReminderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MileageReminderServiceTests {
    @Test void shouldCreateReminderAfterSevenDaysWithoutMileageUpdate() {
        VehicleMileageReminderRepository repository = mock(VehicleMileageReminderRepository.class);
        MileageReminderService service = new MileageReminderService(repository);
        Vehicle vehicle = vehicle(LocalDate.of(2026, 10, 1));
        when(repository.findByVehicleId(vehicle.getId())).thenReturn(Optional.empty());

        service.reconcile(vehicle, LocalDate.of(2026, 10, 7));
        verify(repository, never()).save(any());

        service.reconcile(vehicle, LocalDate.of(2026, 10, 8));
        ArgumentCaptor<VehicleMileageReminder> reminder = ArgumentCaptor.forClass(VehicleMileageReminder.class);
        verify(repository).save(reminder.capture());
        assertEquals(LocalDate.of(2026, 10, 8), reminder.getValue().getReminderDate());
    }

    @Test void shouldNotRepeatReminderWhileTheExistingOneIsPending() {
        VehicleMileageReminderRepository repository = mock(VehicleMileageReminderRepository.class);
        MileageReminderService service = new MileageReminderService(repository);
        Vehicle vehicle = vehicle(LocalDate.of(2026, 10, 1));
        VehicleMileageReminder reminder = new VehicleMileageReminder(vehicle, LocalDate.of(2026, 10, 8));
        reminder.markRead(OffsetDateTime.now(ZoneOffset.UTC));
        when(repository.findByVehicleId(vehicle.getId())).thenReturn(Optional.of(reminder));

        service.reconcile(vehicle, LocalDate.of(2026, 10, 14));
        assertNotNull(reminder.getReadAt());

        service.reconcile(vehicle, LocalDate.of(2026, 10, 15));
        assertEquals(LocalDate.of(2026, 10, 8), reminder.getReminderDate());
        assertNotNull(reminder.getReadAt());
        verify(repository, never()).save(any());
    }

    private Vehicle vehicle(LocalDate readingDate) {
        User user = new User("subject", "driver@example.com", "Driver", null);
        user.setId(UUID.randomUUID());
        Vehicle vehicle = new Vehicle(user);
        vehicle.update(VehicleType.CAR, "Honda", "Fit", 2015, 2016, null, "Meu Fit", null,
                FuelType.FLEX, new BigDecimal("94000"), readingDate, new BigDecimal("10"),
                new BigDecimal("8"), null, null, null);
        return vehicle;
    }
}
