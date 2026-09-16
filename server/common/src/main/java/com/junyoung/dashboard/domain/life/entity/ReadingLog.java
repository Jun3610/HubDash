package com.junyoung.dashboard.domain.life.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "reading_log")
public class ReadingLog extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String author;

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "finished_at")
    private LocalDate finishedAt;

    private Integer rating;

    @Column(length = 1000)
    private String notes;

    public ReadingLog(String title, String author, LocalDate startedAt, LocalDate finishedAt, Integer rating, String notes) {
        this.title = title;
        this.author = author;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.rating = rating;
        this.notes = notes;
    }

    public void update(String title, String author, LocalDate startedAt, LocalDate finishedAt, Integer rating, String notes) {
        this.title = title;
        this.author = author;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.rating = rating;
        this.notes = notes;
    }
}
