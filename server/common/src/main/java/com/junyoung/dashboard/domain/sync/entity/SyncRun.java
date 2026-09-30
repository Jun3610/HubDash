package com.junyoung.dashboard.domain.sync.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 동기화 한 번, 소스 하나의 결과 (이슈 #228) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "sync_run")
public class SyncRun extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private boolean ok;

    @Column(nullable = false)
    private int added;

    @Column(nullable = false)
    private int updated;

    @Column(nullable = false)
    private int deleted;

    @Column(length = 1000)
    private String error;

    public SyncRun(String source, LocalDateTime startedAt, LocalDateTime finishedAt, boolean ok,
                   int added, int updated, int deleted, String error) {
        this.source = source;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.ok = ok;
        this.added = added;
        this.updated = updated;
        this.deleted = deleted;
        this.error = error == null ? null : error.length() <= 1000 ? error : error.substring(0, 1000);
    }
}
