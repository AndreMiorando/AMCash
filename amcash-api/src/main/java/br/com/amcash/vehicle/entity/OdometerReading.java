package br.com.amcash.vehicle.entity;

import jakarta.persistence.*;
import java.math.BigDecimal; import java.time.*; import java.util.UUID;

@Entity @Table(name="vehicle_odometer_readings")
public class OdometerReading {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="vehicle_id", nullable=false) private Vehicle vehicle;
    @Column(nullable=false, precision=12, scale=1) private BigDecimal mileage;
    @Column(name="reading_date", nullable=false) private LocalDate readingDate;
    @Column(nullable=false) private boolean correction;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    protected OdometerReading() {}
    public OdometerReading(Vehicle vehicle, BigDecimal mileage, LocalDate readingDate, boolean correction){id=UUID.randomUUID();this.vehicle=vehicle;this.mileage=mileage;this.readingDate=readingDate;this.correction=correction;createdAt=OffsetDateTime.now(ZoneOffset.UTC);}
    public void update(BigDecimal mileage,LocalDate readingDate,boolean correction){this.mileage=mileage;this.readingDate=readingDate;this.correction=correction;}
    public UUID getId(){return id;} public Vehicle getVehicle(){return vehicle;} public BigDecimal getMileage(){return mileage;} public LocalDate getReadingDate(){return readingDate;} public boolean isCorrection(){return correction;}
}
