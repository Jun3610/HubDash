package com.junyoung.dashboard.domain.health.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_raw_log")
public class RawHealthLog extends BaseEntity {

    @Column(name = "recorded_at_raw", nullable = false, length = 100)
    private String recordedAtRaw;

    @Column(name = "weight_kg_raw", length = 50)
    private String weightKgRaw;

    @Column(name = "sleep_hours_raw")
    private Double sleepHoursRaw;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public RawHealthLog(String recordedAtRaw, String weightKgRaw, Double sleepHoursRaw, String notes) {
        this.recordedAtRaw = recordedAtRaw;
        this.weightKgRaw = weightKgRaw;
        this.sleepHoursRaw = sleepHoursRaw;
        this.notes = notes;
        this.status = RawStatus.PENDING;
    }

    public void markProcessed() {
        this.status = RawStatus.PROCESSED;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = RawStatus.FAILED;
        this.failureReason = reason;
    }
}
