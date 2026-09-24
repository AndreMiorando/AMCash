package br.com.amcash.entry.entity;

import br.com.amcash.user.entity.User;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "financial_entries")
public class FinancialEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EntryCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 16)
    private EntryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", nullable = false, length = 16)
    private RecurrenceFrequency recurrenceFrequency;

    @Column(name = "recurrence_count", nullable = false)
    private int recurrenceCount;

    @Column(name = "recurrence_index", nullable = false)
    private int recurrenceIndex;

    @Column(name = "series_id")
    private UUID seriesId;

    @Column(name = "has_subexpenses", nullable = false)
    private boolean hasSubexpenses;

    @Column(nullable = false)
    private boolean paid;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected FinancialEntry() {
    }

    public FinancialEntry(
            User user,
            String name,
            EntryCategory category,
            EntryType type,
            BigDecimal amount,
            LocalDate dueDate,
            RecurrenceFrequency recurrenceFrequency,
            int recurrenceCount,
            int recurrenceIndex,
            UUID seriesId,
            boolean hasSubexpenses) {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.user = user;
        this.name = name;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.dueDate = dueDate;
        this.recurrenceFrequency = recurrenceFrequency;
        this.recurrenceCount = recurrenceCount;
        this.recurrenceIndex = recurrenceIndex;
        this.seriesId = seriesId;
        this.hasSubexpenses = hasSubexpenses;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String name,
            EntryCategory category,
            EntryType type,
            BigDecimal amount,
            LocalDate dueDate,
            RecurrenceFrequency recurrenceFrequency,
            int recurrenceCount,
            int recurrenceIndex,
            UUID seriesId,
            boolean hasSubexpenses) {

        this.name = name;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.dueDate = dueDate;
        this.recurrenceFrequency = recurrenceFrequency;
        this.recurrenceCount = recurrenceCount;
        this.recurrenceIndex = recurrenceIndex;
        this.seriesId = seriesId;
        this.hasSubexpenses = hasSubexpenses;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public EntryCategory getCategory() { return category; }
    public EntryType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public RecurrenceFrequency getRecurrenceFrequency() { return recurrenceFrequency; }
    public int getRecurrenceCount() { return recurrenceCount; }
    public int getRecurrenceIndex() { return recurrenceIndex; }
    public UUID getSeriesId() { return seriesId; }
    public boolean isHasSubexpenses() { return hasSubexpenses; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) {
        this.paid = paid;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
