package br.com.amcash.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "google_subject", nullable = false, unique = true, length = 255)
    private String googleSubject;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "picture_url", length = 2048)
    private String pictureUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_login_at", nullable = false)
    private OffsetDateTime lastLoginAt;

    protected User() {
    }

    public User(String googleSubject, String email, String name, String pictureUrl) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.googleSubject = googleSubject;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.createdAt = now;
        this.updatedAt = now;
        this.lastLoginAt = now;
    }

    public void updateGoogleProfile(String email, String name, String pictureUrl) {
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.lastLoginAt = this.updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
