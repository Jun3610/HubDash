package com.junyoung.dashboard.domain.pknu.entity;

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
@Table(name = "pknu_raw_assignment")
public class RawAssignment extends BaseEntity {

    @Column(name = "course_id_raw", nullable = false, length = 50)
    private String courseIdRaw;

    @Column(name = "title_raw", nullable = false, length = 200)
    private String titleRaw;

    @Column(name = "due_date_raw", nullable = false, length = 50)
    private String dueDateRaw;

    @Column(name = "completed_raw", nullable = false, length = 50)
    private String completedRaw;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public RawAssignment(String courseIdRaw, String titleRaw, String dueDateRaw, String completedRaw, String notes) {
        this.courseIdRaw = courseIdRaw;
        this.titleRaw = titleRaw;
        this.dueDateRaw = dueDateRaw;
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
