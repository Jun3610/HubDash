package com.junyoung.dashboard.domain.reminder.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "reminder")
public class Reminder extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "target_at", nullable = false)
    private LocalDateTime targetAt;

    @Column(name = "target_domain", length = 50)
    private String targetDomain;

    @Column(name = "target_entity_id")
    private Long targetEntityId;

    @Column(nullable = false)
    private Boolean sent;

    public Reminder(String title, LocalDateTime targetAt, String targetDomain, Long targetEntityId, Boolean sent) {
        this.title = title;
        this.targetAt = targetAt;
        this.targetDomain = targetDomain;
        this.targetEntityId = targetEntityId;
        this.sent = sent;
    }

    public void update(String title, LocalDateTime targetAt, String targetDomain, Long targetEntityId, Boolean sent) {
        this.title = title;
        this.targetAt = targetAt;
        this.targetDomain = targetDomain;
        this.targetEntityId = targetEntityId;
        this.sent = sent;
    }
}
