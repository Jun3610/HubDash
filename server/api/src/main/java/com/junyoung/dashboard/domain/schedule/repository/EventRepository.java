package com.junyoung.dashboard.domain.schedule.repository;

import com.junyoung.dashboard.domain.schedule.entity.Event;
import com.junyoung.dashboard.domain.schedule.entity.EventSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** 동기화 대상 — 그 출처로 가져온 일정만 (이슈 #228) */
    List<Event> findBySource(EventSource source);

    /** 백필 후보 — 직접 만든(출처 없는) 일정 (이슈 #230) */
    List<Event> findBySourceAndExternalIdIsNull(EventSource source);
}
