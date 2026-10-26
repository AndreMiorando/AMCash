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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "work_earning_sources", uniqueConstraints = @UniqueConstraint(
        name = "uk_work_earning_sources_system", columnNames = {"user_id", "system_key"}))
public class WorkEarningSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "system_key", length = 20)
    private String systemKey;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected WorkEarningSource() {
    }

    public WorkEarningSource(User user, String name, String systemKey, int sortOrder) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.user = user;
        this.name = name;
        this.systemKey = systemKey;
        this.active = true;
        this.sortOrder = sortOrder;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void rename(String name) {
        this.name = name;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void activate() {
        this.active = true;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void promoteToSystem(String systemKey, String name, int sortOrder) {
        this.systemKey = systemKey;
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = true;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSystemKey() { return systemKey; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
}
