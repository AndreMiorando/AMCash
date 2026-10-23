package br.com.amcash.vehicle.entity;

import br.com.amcash.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(name = "vehicle_type", nullable = false, length = 16) private VehicleType type;
    @Column(nullable = false, length = 60) private String brand;
    @Column(nullable = false, length = 80) private String model;
    @Column(name = "manufacture_year", nullable = false) private int manufactureYear;
    @Column(name = "model_year", nullable = false) private int modelYear;
    @Column(length = 80) private String version;
    @Column(length = 60) private String nickname;
    @Column(name = "license_plate", length = 10) private String licensePlate;
    @Enumerated(EnumType.STRING) @Column(name = "fuel_type", nullable = false, length = 20) private FuelType fuelType;
    @Column(name = "current_mileage", nullable = false, precision = 12, scale = 1) private BigDecimal currentMileage;
    @Column(name = "mileage_reading_date", nullable = false) private LocalDate mileageReadingDate;
    @Column(name = "gasoline_consumption", precision = 7, scale = 2) private BigDecimal gasolineConsumption;
    @Column(name = "ethanol_consumption", precision = 7, scale = 2) private BigDecimal ethanolConsumption;
    @Column(name = "electric_consumption", precision = 7, scale = 2) private BigDecimal electricConsumption;
    @Column(name = "acquisition_date") private LocalDate acquisitionDate;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected Vehicle() {}
    public Vehicle(User user) { id = UUID.randomUUID(); this.user = user; createdAt = now(); updatedAt = createdAt; }
    public void update(VehicleType type, String brand, String model, int manufactureYear, int modelYear,
                       String version, String nickname, String licensePlate, FuelType fuelType,
                       BigDecimal currentMileage, LocalDate mileageReadingDate, BigDecimal gasolineConsumption,
                       BigDecimal ethanolConsumption, BigDecimal electricConsumption, LocalDate acquisitionDate) {
        this.type=type; this.brand=brand; this.model=model; this.manufactureYear=manufactureYear; this.modelYear=modelYear;
        this.version=version; this.nickname=nickname; this.licensePlate=licensePlate; this.fuelType=fuelType;
        this.currentMileage=currentMileage; this.mileageReadingDate=mileageReadingDate;
        this.gasolineConsumption=gasolineConsumption; this.ethanolConsumption=ethanolConsumption;
        this.electricConsumption=electricConsumption; this.acquisitionDate=acquisitionDate; updatedAt=now();
    }
    public void updateOdometer(BigDecimal mileage, LocalDate readingDate) { currentMileage=mileage; mileageReadingDate=readingDate; updatedAt=now(); }
    private OffsetDateTime now() { return OffsetDateTime.now(ZoneOffset.UTC); }
    public UUID getId(){return id;} public User getUser(){return user;} public VehicleType getType(){return type;}
    public String getBrand(){return brand;} public String getModel(){return model;} public int getManufactureYear(){return manufactureYear;}
    public int getModelYear(){return modelYear;} public String getVersion(){return version;} public String getNickname(){return nickname;}
    public String getLicensePlate(){return licensePlate;} public FuelType getFuelType(){return fuelType;}
    public BigDecimal getCurrentMileage(){return currentMileage;} public LocalDate getMileageReadingDate(){return mileageReadingDate;}
    public BigDecimal getGasolineConsumption(){return gasolineConsumption;} public BigDecimal getEthanolConsumption(){return ethanolConsumption;}
    public BigDecimal getElectricConsumption(){return electricConsumption;} public LocalDate getAcquisitionDate(){return acquisitionDate;}
}
