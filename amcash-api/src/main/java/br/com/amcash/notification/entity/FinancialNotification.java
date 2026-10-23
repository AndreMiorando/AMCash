package br.com.amcash.notification.entity;

import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.user.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "financial_notifications", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entry_id"}))
public class FinancialNotification {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private FinancialEntry entry;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 24)
    private FinancialAlertType alertType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(nullable = false)
    private boolean active;

    protected FinancialNotification() {}

    public FinancialNotification(User user, FinancialEntry entry, FinancialAlertType alertType) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.entry = entry;
        this.alertType = alertType;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.active = true;
    }

    public void activate(FinancialAlertType nextType) {
        if (!active || alertType != nextType) {
            alertType = nextType;
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
            readAt = null;
        }
        active = true;
    }

    public void resolve() { active = false; }
    public void markRead(OffsetDateTime now) { if (readAt == null) readAt = now; }
    public UUID getId() { return id; }
    public FinancialEntry getEntry() { return entry; }
    public FinancialAlertType getAlertType() { return alertType; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getReadAt() { return readAt; }
    public boolean isActive() { return active; }
}
