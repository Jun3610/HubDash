package com.junyoung.dashboard.domain.study.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study_topic_weekly_stat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "week_start"}))
public class StudyTopicWeeklyStat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private StudyTopic topic;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "session_count", nullable = false)
    private Integer sessionCount;

    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes;

    public StudyTopicWeeklyStat(StudyTopic topic, LocalDate weekStart, Integer sessionCount, Integer totalMinutes) {
        this.topic = topic;
        this.weekStart = weekStart;
        this.sessionCount = sessionCount;
        this.totalMinutes = totalMinutes;
    }

    public void updateCounts(Integer sessionCount, Integer totalMinutes) {
        this.sessionCount = sessionCount;
        this.totalMinutes = totalMinutes;
    }
}
