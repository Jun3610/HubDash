package com.junyoung.dashboard.domain.sync.dto;

import java.time.LocalDateTime;
import java.util.List;

/** POST /api/sync 응답 — 연결된 소스마다 한 줄 */
public record SyncResponse(List<SyncSourceResult> sources, LocalDateTime finishedAt) {
}
