package com.junyoung.dashboard.domain.sync.dto;

import java.time.LocalDateTime;
import java.util.List;

/** GET /api/sync/status — 소스별 연결 여부와 마지막 실행 */
public record SyncStatusResponse(List<SourceStatus> sources, boolean running) {

    public record SourceStatus(String name, boolean configured, LastRun lastRun) {
    }

    public record LastRun(LocalDateTime startedAt, LocalDateTime finishedAt, boolean ok,
                          int added, int updated, int deleted, String error) {
    }
}
