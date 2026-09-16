package com.junyoung.dashboard.domain.health.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_workout_log")
public class WorkoutLog extends BaseEntity {

    @Column(name = "performed_at", nullable = false)
    private LocalDate performedAt;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "calories_burned")
    private Integer caloriesBurned;

    @Column(length = 500)
    private String notes;

    public WorkoutLog(LocalDate performedAt, String type, Integer durationMinutes, Integer caloriesBurned, String notes) {
        this.performedAt = performedAt;
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.caloriesBurned = caloriesBurned;
        this.notes = notes;
    }

    public void update(LocalDate performedAt, String type, Integer durationMinutes, Integer caloriesBurned, String notes) {
        this.performedAt = performedAt;
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.caloriesBurned = caloriesBurned;
        this.notes = notes;
    }
}
