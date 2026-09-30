package com.junyoung.dashboard.domain.sync.service;

import com.junyoung.dashboard.domain.schedule.entity.Event;
import com.junyoung.dashboard.domain.schedule.entity.EventSource;
import com.junyoung.dashboard.domain.schedule.repository.EventRepository;
import com.junyoung.dashboard.domain.sync.source.ExternalEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 가져온 일정을 schedule_event에 반영한다 (이슈 #228) — 소스 하나를 한 트랜잭션으로.
 * 그 출처로 가져온 일정만 다룬다: 새 원본 → 추가, 제목·시간·장소가 바뀐 것만 갱신, 원본에서 사라진 것 → 삭제.
 * 직접 만든 일정(MANUAL)은 건드리지 않는다 — 단, 백필로 원본과 짝을 찾은 것만 그 출처로 넘긴다
 * (노션 첫 동기화 때 한 번 #230, iCloud는 매번 #234).
 */
@Component
public class EventImporter {

    private static final Logger log = LoggerFactory.getLogger(EventImporter.class);

    private final EventRepository events;

    public EventImporter(EventRepository events) {
        this.events = events;
    }

    public record Counts(int added, int updated, int deleted, int adopted) {
    }

    @Transactional
    public Counts apply(EventSource source, List<ExternalEvent> fetched, boolean backfill) {
        if (source == EventSource.MANUAL) {
            throw new IllegalArgumentException("MANUAL 일정은 동기화 대상이 아니에요");
        }
        // 같은 원본 ID가 두 번 오면 마지막 것
        Map<String, ExternalEvent> incoming = new LinkedHashMap<>();
        for (ExternalEvent e : fetched) {
            incoming.put(e.externalId(), e);
        }
        Map<String, Event> existing = new HashMap<>();
        for (Event e : events.findBySource(source)) {
            existing.put(e.getExternalId(), e);
        }
        int adopted = backfill ? adopt(source, incoming, existing) : 0;

        // 0건이면 원본 쪽 문제(권한, 잘못된 DB)일 수 있어 한꺼번에 지우지 않는다
        if (incoming.isEmpty() && !existing.isEmpty()) {
            throw new IllegalStateException("가져온 일정이 0건이라 기존 " + existing.size()
                    + "건을 지우지 않았어요 — 원본(노션 DB, 캘린더 선택)을 확인해 주세요");
        }

        int added = 0;
        int updated = 0;
        for (ExternalEvent e : incoming.values()) {
            Event current = existing.remove(e.externalId());
            if (current == null) {
                events.save(Event.imported(source, e.externalId(), e.title(), e.startAt(), e.endAt(), e.location(), e.allDay()));
                added++;
            } else if (!current.sameAsImported(e.title(), e.startAt(), e.endAt(), e.location(), e.allDay())) {
                current.applyImported(e.title(), e.startAt(), e.endAt(), e.location(), e.allDay());
                updated++;
            }
        }
        events.deleteAll(existing.values());
        return new Counts(added, updated, existing.size(), adopted);
    }

    /**
     * 백필 (이슈 #230, #234): 출처가 없는 직접 일정(노션 이관분 등)이 원본에도 있으면 그대로 두면 중복된다.
     * (제목, KST 시작 시각)이 같은 직접 일정(MANUAL, 원본 ID 없음)을 원본 일정과 1:1로 짝지어 출처를 채운다.
     * 짝지어진 것은 existing에 넣어, 이어지는 갱신 단계가 새로 만들지 않고 비교만 하게 한다.
     */
    private int adopt(EventSource source, Map<String, ExternalEvent> incoming, Map<String, Event> existing) {
        Map<String, Deque<Event>> manual = new HashMap<>();
        for (Event e : events.findBySourceAndExternalIdIsNull(EventSource.MANUAL)) {
            manual.computeIfAbsent(key(e.getTitle(), e.getStartAt()), k -> new ArrayDeque<>()).add(e);
        }
        int total = manual.values().stream().mapToInt(Deque::size).sum();
        int matched = 0;
        for (ExternalEvent e : incoming.values()) {
            if (existing.containsKey(e.externalId())) {
                continue;
            }
            Deque<Event> candidates = manual.get(key(e.title(), e.startAt()));
            if (candidates != null && !candidates.isEmpty()) {
                Event event = candidates.poll();
                event.adopt(source, e.externalId());
                existing.put(e.externalId(), event);
                matched++;
            }
        }
        List<String> unmatched = new ArrayList<>();
        manual.values().forEach(left -> left.forEach(ev -> unmatched.add(ev.getStartAt() + " " + ev.getTitle())));
        // 노션 백필은 한 번뿐이라 결과를 다 남기고, iCloud는 동기화마다 돌아 짝지은 게 있을 때만 요약(목록은 DEBUG)
        boolean once = source == EventSource.NOTION;
        if (once || matched > 0) {
            log.info("{} 백필: 직접 일정 {}건 중 {}건을 원본과 짝지음, 남은 {}건은 MANUAL로 둠", source, total, matched, unmatched.size());
        }
        if (!unmatched.isEmpty()) {
            if (once) {
                log.info("{} 백필 미매칭: {}", source, unmatched);
            } else {
                log.debug("{} 백필 미매칭: {}", source, unmatched);
            }
        }
        return matched;
    }

    private static String key(String title, LocalDateTime startAt) {
        return (title == null ? "" : title.strip()) + "\u0000" + startAt;
    }
}
