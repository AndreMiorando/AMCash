package br.com.amcash.entry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(name = "recurrence_frequency", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency recurrenceFrequency;

    @Column(name = "recurrence_count", nullable = false)
    private int recurrenceCount;

    @Column(name = "recurrence_index", nullable = false)
    private int recurrenceIndex;

    @Column(name = "series_id")
    private UUID seriesId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Subexpense() {
    }

    public Subexpense(FinancialEntry entry, String name, BigDecimal amount, String installmentDescription, boolean paid) {
        this(entry, name, amount, installmentDescription, paid, RecurrenceFrequency.NONE, 0, 0, null);
    }

    public Subexpense(
            FinancialEntry entry,
            String name,
            BigDecimal amount,
            String installmentDescription,
            boolean paid,
            RecurrenceFrequency recurrenceFrequency,
            int recurrenceCount,
            int recurrenceIndex,
            UUID seriesId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.entry = entry;
        this.name = name;
        this.amount = amount;
        this.installmentDescription = installmentDescription;
        this.paid = paid;
        this.recurrenceFrequency = recurrenceFrequency;
        this.recurrenceCount = recurrenceCount;
        this.recurrenceIndex = recurrenceIndex;
        this.seriesId = seriesId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, BigDecimal amount, String installmentDescription, boolean paid) {
        update(name, amount, installmentDescription, paid, recurrenceFrequency, recurrenceCount);
    }

    public void update(
            String name,
            BigDecimal amount,
            String installmentDescription,
            boolean paid,
            RecurrenceFrequency recurrenceFrequency,
            int recurrenceCount) {
        synchronize(
                entry,
                name,
                amount,
                installmentDescription,
                paid,
                recurrenceFrequency,
                recurrenceCount,
                recurrenceIndex,
                seriesId);
    }

    public void synchronize(
            FinancialEntry entry,
            String name,
            BigDecimal amount,
            String installmentDescription,
            boolean paid,
            RecurrenceFrequency recurrenceFrequency,
            int recurrenceCount,
            int recurrenceIndex,
            UUID seriesId) {
        this.entry = entry;
        this.name = name;
        this.amount = amount;
        this.installmentDescription = installmentDescription;
        this.paid = paid;
        this.recurrenceFrequency = recurrenceFrequency;
        this.recurrenceCount = recurrenceCount;
        this.recurrenceIndex = recurrenceIndex;
        this.seriesId = seriesId;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public FinancialEntry getEntry() { return entry; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public String getInstallmentDescription() { return installmentDescription; }
    public boolean isPaid() { return paid; }
    public RecurrenceFrequency getRecurrenceFrequency() { return recurrenceFrequency; }
    public int getRecurrenceCount() { return recurrenceCount; }
    public int getRecurrenceIndex() { return recurrenceIndex; }
    public UUID getSeriesId() { return seriesId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
