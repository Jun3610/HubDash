package com.junyoung.dashboard.domain.reminder.entity;

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
@Table(name = "reminder_raw")
public class RawReminder extends BaseEntity {

    @Column(name = "title_raw", nullable = false, length = 200)
    private String titleRaw;

    @Column(name = "target_at_raw", nullable = false, length = 50)
    private String targetAtRaw;

    @Column(name = "target_domain", length = 50)
    private String targetDomain;

    @Column(name = "target_entity_id")
    private Long targetEntityId;

    @Column(name = "sent_raw", nullable = false, length = 10)
    private String sentRaw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public RawReminder(String titleRaw, String targetAtRaw, String targetDomain, Long targetEntityId, String sentRaw) {
        this.titleRaw = titleRaw;
        this.targetAtRaw = targetAtRaw;
        this.targetDomain = targetDomain;
        this.targetEntityId = targetEntityId;
        this.sentRaw = sentRaw;
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
