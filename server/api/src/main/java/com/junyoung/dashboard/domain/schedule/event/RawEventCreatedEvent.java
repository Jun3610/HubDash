package com.junyoung.dashboard.domain.schedule.event;

// Kafka 이벤트 페이로드는 id만 담는다 — Consumer가 필요하면 DB에서 재조회한다.
public record RawEventCreatedEvent(Long rawEventId) {
}
