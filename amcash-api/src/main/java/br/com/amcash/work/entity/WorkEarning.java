package br.com.amcash.work.entity;

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
@Table(name = "work_earnings")
public class WorkEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_day_id", nullable = false)
    private WorkDay workDay;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected WorkEarning() {
    }

    public WorkEarning(WorkDay workDay, String source, BigDecimal amount, int sortOrder) {
        this.workDay = workDay;
        this.source = source;
        this.amount = amount;
        this.sortOrder = sortOrder;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public WorkDay getWorkDay() { return workDay; }
    public String getSource() { return source; }
    public BigDecimal getAmount() { return amount; }
    public int getSortOrder() { return sortOrder; }
}
