package com.junyoung.dashboard.domain.schedule.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "schedule_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_schedule_event_source_external", columnNames = {"source", "external_id"}))
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

    /** 어디서 온 일정인지 (이슈 #228) — 직접 만든 일정은 MANUAL */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventSource source = EventSource.MANUAL;

    /** 노션 페이지 ID, iCloud UID(반복이면 UID@발생 시각) — MANUAL은 없음 */
    @Column(name = "external_id", length = 200)
    private String externalId;

    public Event(String title, LocalDateTime startAt, LocalDateTime endAt, String location, String description, Boolean allDay) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.description = description;
        this.allDay = allDay;
    }

    /** 노션·iCloud에서 가져온 일정 (이슈 #228) */
    public static Event imported(EventSource source, String externalId, String title, LocalDateTime startAt,
                                 LocalDateTime endAt, String location, Boolean allDay) {
        Event event = new Event(title, startAt, endAt, location, null, allDay);
        event.source = source;
        event.externalId = externalId;
        return event;
    }

    /** 원본과 제목·시간·장소가 같은지 — 다를 때만 갱신한다 */
    public boolean sameAsImported(String title, LocalDateTime startAt, LocalDateTime endAt, String location, Boolean allDay) {
        return Objects.equals(this.title, title) && Objects.equals(this.startAt, startAt)
                && Objects.equals(this.endAt, endAt) && Objects.equals(this.location, location)
                && Objects.equals(this.allDay, allDay);
    }

    /** 원본이 바뀐 만큼만 덮어쓴다 — 설명(description)은 HubDash에서 적은 것을 남긴다 */
    public void applyImported(String title, LocalDateTime startAt, LocalDateTime endAt, String location, Boolean allDay) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.allDay = allDay;
    }

    /** 노션에서 옮겨 둔 직접 일정을 원본과 짝짓는다 (백필, 이슈 #230) */
    public void adopt(EventSource source, String externalId) {
        this.source = source;
        this.externalId = externalId;
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
