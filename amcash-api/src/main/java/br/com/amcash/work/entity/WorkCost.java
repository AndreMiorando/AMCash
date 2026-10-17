package br.com.amcash.work.entity;

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
@Table(name = "work_costs")
public class WorkCost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "cost_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private WorkCostCategory category;

    @Column(length = 160)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected WorkCost() {
    }

    public WorkCost(User user, LocalDate date, WorkCostCategory category, String description, BigDecimal amount) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.user = user;
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(LocalDate date, WorkCostCategory category, String description, BigDecimal amount) {
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public LocalDate getDate() { return date; }
    public WorkCostCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
}
