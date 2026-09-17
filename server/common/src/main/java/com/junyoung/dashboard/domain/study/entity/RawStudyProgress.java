package com.junyoung.dashboard.domain.study.entity;

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
@Table(name = "study_raw_progress")
public class RawStudyProgress extends BaseEntity {

    @Column(name = "topic_id_raw", nullable = false, length = 50)
    private String topicIdRaw;

    @Column(name = "studied_at_raw", nullable = false, length = 50)
    private String studiedAtRaw;

    @Column(name = "minutes_raw", nullable = false, length = 50)
    private String minutesRaw;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public RawStudyProgress(String topicIdRaw, String studiedAtRaw, String minutesRaw, String notes) {
        this.topicIdRaw = topicIdRaw;
        this.studiedAtRaw = studiedAtRaw;
        this.minutesRaw = minutesRaw;
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
