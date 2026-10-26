package br.com.amcash.operationalcost.entity;

import br.com.amcash.user.entity.User;
import br.com.amcash.vehicle.entity.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="operational_cost_configurations",uniqueConstraints=@UniqueConstraint(columnNames="vehicle_id"))
public class OperationalCostConfiguration {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) private Vehicle vehicle;
    @Column(name="monthly_mileage",nullable=false,precision=12,scale=2) private BigDecimal monthlyMileage;
    @Enumerated(EnumType.STRING) @Column(name="mileage_source",nullable=false,length=16) private MileageSource mileageSource=MileageSource.MANUAL;
    @Enumerated(EnumType.STRING) @Column(name="fuel_type",nullable=false,length=20) private FuelType fuelType;
    @Column(name="fuel_price",nullable=false,precision=19,scale=4) private BigDecimal fuelPrice;
    @Column(name="include_finance",nullable=false) private boolean includeFinance;
    @Column(name="annual_ipva",nullable=false,precision=19,scale=2) private BigDecimal annualIpva;
    @Column(name="annual_fees",nullable=false,precision=19,scale=2) private BigDecimal annualFees;
    @Column(name="insurance_amount",precision=19,scale=2) private BigDecimal insuranceAmount;
    @Enumerated(EnumType.STRING) @Column(name="insurance_period",length=10) private CostPeriod insurancePeriod;
    @Enumerated(EnumType.STRING) @Column(name="maintenance_method",nullable=false,length=16) private MaintenanceCalculationMethod maintenanceMethod;
    @Column(name="annual_maintenance_estimate",precision=19,scale=2) private BigDecimal annualMaintenanceEstimate;
    @Column(name="include_depreciation",nullable=false) private boolean includeDepreciation;
    @Column(name="current_vehicle_value",precision=19,scale=2) private BigDecimal currentVehicleValue;
    @Column(name="annual_depreciation_rate",precision=7,scale=4) private BigDecimal annualDepreciationRate;
    @ElementCollection @CollectionTable(name="operational_cost_custom_fixed_costs",joinColumns=@JoinColumn(name="configuration_id"))
    @OrderColumn(name="item_order") private List<OperationalCustomFixedCost> customFixedCosts=new ArrayList<>();
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    protected OperationalCostConfiguration() {}
    public OperationalCostConfiguration(User user,Vehicle vehicle){id=UUID.randomUUID();this.user=user;this.vehicle=vehicle;createdAt=OffsetDateTime.now(ZoneOffset.UTC);updatedAt=createdAt;}
    public void update(BigDecimal monthlyMileage,MileageSource mileageSource,FuelType fuelType,BigDecimal fuelPrice,boolean includeFinance,BigDecimal annualIpva,BigDecimal annualFees,BigDecimal insuranceAmount,CostPeriod insurancePeriod,MaintenanceCalculationMethod maintenanceMethod,BigDecimal annualMaintenanceEstimate,boolean includeDepreciation,BigDecimal currentVehicleValue,BigDecimal annualDepreciationRate,List<OperationalCustomFixedCost> customFixedCosts){this.monthlyMileage=monthlyMileage;this.mileageSource=mileageSource;this.fuelType=fuelType;this.fuelPrice=fuelPrice;this.includeFinance=includeFinance;this.annualIpva=annualIpva;this.annualFees=annualFees;this.insuranceAmount=insuranceAmount;this.insurancePeriod=insurancePeriod;this.maintenanceMethod=maintenanceMethod;this.annualMaintenanceEstimate=annualMaintenanceEstimate;this.includeDepreciation=includeDepreciation;this.currentVehicleValue=currentVehicleValue;this.annualDepreciationRate=annualDepreciationRate;this.customFixedCosts.clear();this.customFixedCosts.addAll(customFixedCosts);updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
    public void updateMileage(BigDecimal monthlyMileage,MileageSource mileageSource){this.monthlyMileage=monthlyMileage;this.mileageSource=mileageSource;updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
    public UUID getId(){return id;} public User getUser(){return user;} public Vehicle getVehicle(){return vehicle;} public BigDecimal getMonthlyMileage(){return monthlyMileage;} public MileageSource getMileageSource(){return mileageSource;} public FuelType getFuelType(){return fuelType;} public BigDecimal getFuelPrice(){return fuelPrice;} public boolean isIncludeFinance(){return includeFinance;} public BigDecimal getAnnualIpva(){return annualIpva;} public BigDecimal getAnnualFees(){return annualFees;} public BigDecimal getInsuranceAmount(){return insuranceAmount;} public CostPeriod getInsurancePeriod(){return insurancePeriod;} public MaintenanceCalculationMethod getMaintenanceMethod(){return maintenanceMethod;} public BigDecimal getAnnualMaintenanceEstimate(){return annualMaintenanceEstimate;} public boolean isIncludeDepreciation(){return includeDepreciation;} public BigDecimal getCurrentVehicleValue(){return currentVehicleValue;} public BigDecimal getAnnualDepreciationRate(){return annualDepreciationRate;} public List<OperationalCustomFixedCost> getCustomFixedCosts(){return customFixedCosts;}
}
