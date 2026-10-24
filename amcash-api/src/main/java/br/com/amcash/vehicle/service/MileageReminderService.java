package br.com.amcash.vehicle.service;

import br.com.amcash.notification.dto.NotificationResponse;
import br.com.amcash.vehicle.entity.*;
import br.com.amcash.vehicle.repository.VehicleMileageReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MileageReminderService {
    static final int REMINDER_INTERVAL_DAYS = 7;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final VehicleMileageReminderRepository repository;

    public MileageReminderService(VehicleMileageReminderRepository repository) { this.repository = repository; }

    @Transactional public void reconcile(Vehicle vehicle) { reconcile(vehicle, LocalDate.now(BUSINESS_ZONE)); }

    void reconcile(Vehicle vehicle, LocalDate today) {
        LocalDate readingDate = vehicle.getMileageReadingDate();
        Optional<VehicleMileageReminder> current = repository.findByVehicleId(vehicle.getId());
        long elapsedDays = ChronoUnit.DAYS.between(readingDate, today);
        if (elapsedDays < REMINDER_INTERVAL_DAYS) {
            current.ifPresent(VehicleMileageReminder::resolve);
            return;
        }
        LocalDate reminderDate = readingDate.plusDays(REMINDER_INTERVAL_DAYS);
        if (current.isEmpty()) repository.save(new VehicleMileageReminder(vehicle, reminderDate));
        else if (!current.get().isActive()) current.get().activate(reminderDate);
    }

    @Transactional public void resolve(Vehicle vehicle) {
        repository.findByVehicleId(vehicle.getId()).ifPresent(VehicleMileageReminder::resolve);
    }

    @Transactional public void deleteForVehicle(UUID vehicleId) { repository.deleteAllByVehicleId(vehicleId); }

    @Transactional(readOnly = true) public List<NotificationResponse> list(UUID userId) {
        return repository.findAllByUserIdAndActiveTrue(userId).stream().map(this::response).toList();
    }

    @Transactional public List<NotificationResponse> markAllRead(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<VehicleMileageReminder> current = repository.findAllByUserIdAndActiveTrue(userId);
        current.forEach(item -> item.markRead(now));
        return current.stream().map(this::response).toList();
    }

    private NotificationResponse response(VehicleMileageReminder reminder) {
        Vehicle vehicle = reminder.getVehicle();
        String vehicleName = vehicle.getNickname() != null ? vehicle.getNickname() : vehicle.getBrand() + " " + vehicle.getModel();
        String message = "Última leitura em " + vehicle.getMileageReadingDate().format(DATE_FORMAT) + ". Atualize a quilometragem do veículo.";
        return new NotificationResponse(reminder.getId(), "MILEAGE_UPDATE", null, vehicleName, null,
                LocalDate.now(BUSINESS_ZONE), reminder.getCreatedAt(), reminder.getReadAt(), "VEHICLE", vehicle.getId(), message);
    }
}
