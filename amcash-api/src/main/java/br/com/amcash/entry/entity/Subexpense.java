package br.com.amcash.entry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "subexpenses")
public class Subexpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private FinancialEntry entry;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "installment_description", length = 80)
    private String installmentDescription;

    @Column(nullable = false)
    private boolean paid;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Subexpense() {
    }

    public Subexpense(FinancialEntry entry, String name, BigDecimal amount, String installmentDescription, boolean paid) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.entry = entry;
        this.name = name;
        this.amount = amount;
        this.installmentDescription = installmentDescription;
        this.paid = paid;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, BigDecimal amount, String installmentDescription, boolean paid) {
        this.name = name;
        this.amount = amount;
        this.installmentDescription = installmentDescription;
        this.paid = paid;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public FinancialEntry getEntry() { return entry; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public String getInstallmentDescription() { return installmentDescription; }
    public boolean isPaid() { return paid; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
