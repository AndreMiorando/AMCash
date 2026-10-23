package br.com.amcash.goal.entity;

import br.com.amcash.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "monthly_goal_configurations")
public class MonthlyGoalConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "include_expenses", nullable = false)
    private boolean includeExpenses;

    @Column(name = "additional_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal additionalAmount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MonthlyGoalConfiguration() {
    }

    public MonthlyGoalConfiguration(User user, boolean includeExpenses, BigDecimal additionalAmount) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.user = user;
        this.includeExpenses = includeExpenses;
        this.additionalAmount = additionalAmount;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(boolean includeExpenses, BigDecimal additionalAmount) {
        this.includeExpenses = includeExpenses;
        this.additionalAmount = additionalAmount;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public boolean isIncludeExpenses() { return includeExpenses; }
    public BigDecimal getAdditionalAmount() { return additionalAmount; }
}
