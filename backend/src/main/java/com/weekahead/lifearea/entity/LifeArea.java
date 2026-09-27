package com.weekahead.lifearea.entity;

import java.time.Instant;

import com.weekahead.auth.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "life_areas")
public class LifeArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Integer weight;

    @Column(name = "min_minutes", nullable = false)
    private Integer minMinutes;

    @Column(name = "max_minutes", nullable = false)
    private Integer maxMinutes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    protected LifeArea() {
        // Required by JPA
    }

    public LifeArea(
            User user,
            String name,
            String description,
            Integer weight,
            Integer minMinutes,
            Integer maxMinutes
    ) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.minMinutes = minMinutes;
        this.maxMinutes = maxMinutes;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Integer getWeight() {
        return weight;
    }

    public Integer getMinMinutes() {
        return minMinutes;
    }

    public Integer getMaxMinutes() {
        return maxMinutes;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public void setMinMinutes(Integer minMinutes) {
        this.minMinutes = minMinutes;
    }

    public void setMaxMinutes(Integer maxMinutes) {
        this.maxMinutes = maxMinutes;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

}
