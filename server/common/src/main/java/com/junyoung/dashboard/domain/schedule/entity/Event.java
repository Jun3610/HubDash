package com.junyoung.dashboard.domain.schedule.entity;

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
@Table(name = "schedule_event")
public class Event extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    /** 끝나는 시각은 선택 (이슈 #156) */
    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(length = 200)
    private String location;

    @Column(length = 1000)
    private String description;

    @Column(name = "all_day", nullable = false)
    private Boolean allDay;

    public Event(String title, LocalDateTime startAt, LocalDateTime endAt, String location, String description, Boolean allDay) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.description = description;
        this.allDay = allDay;
    }

    public void update(String title, LocalDateTime startAt, LocalDateTime endAt, String location, String description, Boolean allDay) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.description = description;
        this.allDay = allDay;
    }
}
