package com.junyoung.dashboard.domain.sync.source;

import java.time.LocalDateTime;

/** 외부(노션, iCloud)에서 읽어 HubDash 일정 모양으로 바꾼 한 건 — 시간은 KST (이슈 #228) */
public record ExternalEvent(String externalId, String title, LocalDateTime startAt, LocalDateTime endAt,
                            String location, boolean allDay) {
}
