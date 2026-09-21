package com.weekahead.timetracking.entity;

import com.weekahead.auth.entity.User;
import com.weekahead.lifearea.entity.LifeArea;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_logs")
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "life_area_id", nullable = false)
    private LifeArea lifeArea;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TimeLog() {
    }

    public TimeLog(
            User user,
            LifeArea lifeArea,
            LocalDate logDate,
            Integer durationMinutes,
            String note,
            String source
    ) {
        this.user = user;
        this.lifeArea = lifeArea;
        this.logDate = logDate;
        this.durationMinutes = durationMinutes;
        this.note = note;
        this.source = source;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LifeArea getLifeArea() {
        return lifeArea;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public String getNote() {
        return note;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(
            LifeArea lifeArea,
            LocalDate logDate,
            Integer durationMinutes,
            String note,
            String source
    ) {
        this.lifeArea = lifeArea;
        this.logDate = logDate;
        this.durationMinutes = durationMinutes;
        this.note = note;
        this.source = source;
    }
}