package br.com.amcash.vehicle.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="vehicle_finances")
public class VehicleFinance {
    @Id @Column(name="vehicle_id") private java.util.UUID vehicleId;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @MapsId @JoinColumn(name="vehicle_id") private Vehicle vehicle;
    @Enumerated(EnumType.STRING) @Column(name="financial_status", nullable=false, length=20) private FinancialStatus status;
    @Column(name="monthly_amount", precision=19, scale=2) private BigDecimal monthlyAmount;
    @Column(name="total_installments") private Integer totalInstallments;
    @Column(name="paid_installments") private Integer paidInstallments;
    @Column(name="next_due_date") private LocalDate nextDueDate;
    @Column(name="updated_at", nullable=false) private OffsetDateTime updatedAt;
    protected VehicleFinance() {}
    public VehicleFinance(Vehicle vehicle){this.vehicle=vehicle;}
    public void update(FinancialStatus status, BigDecimal monthlyAmount, Integer totalInstallments, Integer paidInstallments, LocalDate nextDueDate){
        this.status=status; this.monthlyAmount=monthlyAmount; this.totalInstallments=totalInstallments; this.paidInstallments=paidInstallments; this.nextDueDate=nextDueDate; updatedAt=OffsetDateTime.now(ZoneOffset.UTC);
    }
    public FinancialStatus getStatus(){return status;} public BigDecimal getMonthlyAmount(){return monthlyAmount;}
    public Integer getTotalInstallments(){return totalInstallments;} public Integer getPaidInstallments(){return paidInstallments;}
    public LocalDate getNextDueDate(){return nextDueDate;}
}
