package br.com.amcash.vehicle.entity;

import jakarta.persistence.*;
import java.math.BigDecimal; import java.time.*; import java.util.UUID;

@Entity @Table(name="vehicle_maintenances")
public class VehicleMaintenance {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="vehicle_id", nullable=false) private Vehicle vehicle;
    @Enumerated(EnumType.STRING) @Column(name="maintenance_type", nullable=false, length=40) private MaintenanceType type;
    @Column(name="custom_name", length=80) private String customName;
    @Column(name="maintenance_date", nullable=false) private LocalDate date;
    @Column(name="service_mileage", nullable=false, precision=12, scale=1) private BigDecimal mileage;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal amount;
    @Column(length=300) private String description;
    @Column(name="next_mileage", precision=12, scale=1) private BigDecimal nextMileage;
    @Column(name="next_date") private LocalDate nextDate;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false) private OffsetDateTime updatedAt;
    protected VehicleMaintenance() {}
    public VehicleMaintenance(Vehicle vehicle){id=UUID.randomUUID();this.vehicle=vehicle;createdAt=OffsetDateTime.now(ZoneOffset.UTC);updatedAt=createdAt;}
    public void update(MaintenanceType type,String customName,LocalDate date,BigDecimal mileage,BigDecimal amount,String description,BigDecimal nextMileage,LocalDate nextDate){this.type=type;this.customName=customName;this.date=date;this.mileage=mileage;this.amount=amount;this.description=description;this.nextMileage=nextMileage;this.nextDate=nextDate;updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
    public UUID getId(){return id;} public Vehicle getVehicle(){return vehicle;} public MaintenanceType getType(){return type;} public String getCustomName(){return customName;}
    public LocalDate getDate(){return date;} public BigDecimal getMileage(){return mileage;} public BigDecimal getAmount(){return amount;} public String getDescription(){return description;}
    public BigDecimal getNextMileage(){return nextMileage;} public LocalDate getNextDate(){return nextDate;}
}
