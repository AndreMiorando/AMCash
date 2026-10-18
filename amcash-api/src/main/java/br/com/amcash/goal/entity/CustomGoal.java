package br.com.amcash.goal.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "custom_goals")
public class CustomGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CustomGoalPeriod period;

    @Column(name = "tracked_on_home", nullable = false)
    private boolean trackedOnHome;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CustomGoal() {
    }

    public CustomGoal(User user, String name, BigDecimal targetAmount, CustomGoalPeriod period) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.user = user;
        this.name = name;
        this.targetAmount = targetAmount;
        this.period = period;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, BigDecimal targetAmount, CustomGoalPeriod period) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.period = period;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public CustomGoalPeriod getPeriod() { return period; }
    public boolean isTrackedOnHome() { return trackedOnHome; }
    public void updateTrackedOnHome(boolean trackedOnHome) {
        this.trackedOnHome = trackedOnHome;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
