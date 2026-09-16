package com.junyoung.dashboard.domain.study.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study_progress")
public class StudyProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private StudyTopic topic;

    @Column(name = "studied_at", nullable = false)
    private LocalDate studiedAt;

    @Column(nullable = false)
    private Integer minutes;

    @Column(length = 1000)
    private String notes;

    public StudyProgress(StudyTopic topic, LocalDate studiedAt, Integer minutes, String notes) {
        this.topic = topic;
        this.studiedAt = studiedAt;
        this.minutes = minutes;
        this.notes = notes;
    }

    public void update(StudyTopic topic, LocalDate studiedAt, Integer minutes, String notes) {
        this.topic = topic;
        this.studiedAt = studiedAt;
        this.minutes = minutes;
        this.notes = notes;
    }
}
