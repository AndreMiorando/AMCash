package br.com.amcash.vehicle.entity;

import br.com.amcash.user.entity.User;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "vehicle_mileage_reminders")
public class VehicleMileageReminder {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vehicle_id", nullable = false, unique = true) private Vehicle vehicle;
    @Column(name = "reminder_date", nullable = false) private LocalDate reminderDate;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "read_at") private OffsetDateTime readAt;
    @Column(nullable = false) private boolean active;

    protected VehicleMileageReminder() {}

    public VehicleMileageReminder(Vehicle vehicle, LocalDate reminderDate) {
        id = UUID.randomUUID();
        user = vehicle.getUser();
        this.vehicle = vehicle;
        activate(reminderDate);
    }

    public void activate(LocalDate reminderDate) {
        if (!active || !reminderDate.equals(this.reminderDate)) {
            this.reminderDate = reminderDate;
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
            readAt = null;
        }
        active = true;
    }

    public void resolve() { active = false; }
    public void markRead(OffsetDateTime readAt) { if (this.readAt == null) this.readAt = readAt; }
    public UUID getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public LocalDate getReminderDate() { return reminderDate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getReadAt() { return readAt; }
}
