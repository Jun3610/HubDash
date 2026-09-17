package com.junyoung.dashboard.domain.schedule.entity;

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
@Table(name = "schedule_raw_event")
public class RawEvent extends BaseEntity {

    @Column(name = "title_raw", nullable = false, length = 200)
    private String titleRaw;

    @Column(name = "start_at_raw", nullable = false, length = 100)
    private String startAtRaw;

    @Column(name = "end_at_raw", nullable = false, length = 100)
    private String endAtRaw;

    @Column(length = 200)
    private String location;

    @Column(length = 1000)
    private String description;

    @Column(name = "all_day_raw", nullable = false, length = 20)
    private String allDayRaw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public RawEvent(String titleRaw, String startAtRaw, String endAtRaw, String location, String description, String allDayRaw) {
        this.titleRaw = titleRaw;
        this.startAtRaw = startAtRaw;
        this.endAtRaw = endAtRaw;
        this.location = location;
        this.description = description;
        this.allDayRaw = allDayRaw;
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
