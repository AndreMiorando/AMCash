package br.com.amcash.work.entity;

import br.com.amcash.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "work_days", uniqueConstraints = @UniqueConstraint(
        name = "uk_work_days_user_date", columnNames = {"user_id", "work_date"}))
public class WorkDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "work_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal kilometers;

    @Column(name = "fuel_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal fuelCost;

    @Column(name = "other_costs", nullable = false, precision = 19, scale = 2)
    private BigDecimal otherCosts;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected WorkDay() {
    }

    public WorkDay(User user, LocalDate date, BigDecimal kilometers) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.user = user;
        this.date = date;
        this.kilometers = kilometers;
        this.fuelCost = BigDecimal.ZERO;
        this.otherCosts = BigDecimal.ZERO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(BigDecimal kilometers) {
        this.kilometers = kilometers;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getDate() { return date; }
    public BigDecimal getKilometers() { return kilometers; }
}
