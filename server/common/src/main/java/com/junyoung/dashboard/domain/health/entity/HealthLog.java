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
@Table(name = "health_log")
public class HealthLog extends BaseEntity {

    @Column(name = "recorded_at", nullable = false)
    private LocalDate recordedAt;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "sleep_hours")
    private Double sleepHours;

    @Column(length = 500)
    private String notes;

    public HealthLog(LocalDate recordedAt, Double weightKg, Double sleepHours, String notes) {
        this.recordedAt = recordedAt;
        this.weightKg = weightKg;
        this.sleepHours = sleepHours;
        this.notes = notes;
    }

    public void update(LocalDate recordedAt, Double weightKg, Double sleepHours, String notes) {
        this.recordedAt = recordedAt;
        this.weightKg = weightKg;
        this.sleepHours = sleepHours;
        this.notes = notes;
    }
}
