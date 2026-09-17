package com.junyoung.dashboard.domain.life.entity;

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
@Table(name = "life_raw_habit_log")
public class RawHabitLog extends BaseEntity {

    @Column(name = "habit_id_raw", nullable = false, length = 50)
    private String habitIdRaw;

    @Column(name = "performed_at_raw", nullable = false, length = 50)
    private String performedAtRaw;

    @Column(name = "completed_raw", nullable = false, length = 50)
    private String completedRaw;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public RawHabitLog(String habitIdRaw, String performedAtRaw, String completedRaw, String notes) {
        this.habitIdRaw = habitIdRaw;
        this.performedAtRaw = performedAtRaw;
        this.completedRaw = completedRaw;
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
