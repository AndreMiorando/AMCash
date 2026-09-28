package br.com.amcash.goal.entity;

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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(
        name = "goal_month_preferences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_goal_month_preferences_user_period",
                columnNames = {"user_id", "goal_year", "goal_month"}))
public class GoalMonthPreference {

    public static final int ALL_WEEKDAYS_MASK = 127;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "goal_year", nullable = false)
    private int year;

    @Column(name = "goal_month", nullable = false)
    private int month;

    @Column(name = "weekdays_mask", nullable = false)
    private int weekdaysMask;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected GoalMonthPreference() {
    }

    public GoalMonthPreference(User user, int year, int month, int weekdaysMask) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.user = user;
        this.year = year;
        this.month = month;
        this.weekdaysMask = weekdaysMask;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getWeekdaysMask() {
        return weekdaysMask;
    }

    public void updateWeekdaysMask(int weekdaysMask) {
        this.weekdaysMask = weekdaysMask;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
