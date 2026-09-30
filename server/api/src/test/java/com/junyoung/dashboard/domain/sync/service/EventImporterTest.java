package com.junyoung.dashboard.domain.sync.service;

import com.junyoung.dashboard.domain.schedule.entity.Event;
import com.junyoung.dashboard.domain.schedule.entity.EventSource;
import com.junyoung.dashboard.domain.schedule.repository.EventRepository;
import com.junyoung.dashboard.domain.sync.source.ExternalEvent;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 JPA(H2)로 upsert·삭제·MANUAL 불변·백필을 확인한다 (이슈 #228, #230)
@DataJpaTest
@Import({JpaAuditingConfig.class, EventImporter.class})
@ActiveProfiles("test")
class EventImporterTest {

    private static final LocalDateTime NINE = LocalDateTime.of(2026, 9, 24, 9, 0);

    @Autowired
    private EventImporter importer;
    @Autowired
    private EventRepository events;

    private static ExternalEvent ext(String id, String title, LocalDateTime start) {
        return new ExternalEvent(id, title, start, null, null, false);
    }

    private Event find(EventSource source, String externalId) {
        return events.findBySource(source).stream().filter(e -> externalId.equals(e.getExternalId())).findFirst().orElseThrow();
    }

    @Test
    void addsThenIsIdempotentThenUpdatesOnlyChangedAndDeletesMissing() {
        List<ExternalEvent> first = List.of(ext("a", "출근", NINE), ext("b", "미용실", NINE.plusHours(5)));
        assertThat(importer.apply(EventSource.NOTION, first, false)).isEqualTo(new EventImporter.Counts(2, 0, 0, 0));

        // 같은 내용으로 다시 → 아무것도 안 바뀜
        assertThat(importer.apply(EventSource.NOTION, first, false)).isEqualTo(new EventImporter.Counts(0, 0, 0, 0));
        assertThat(events.findBySource(EventSource.NOTION)).hasSize(2);

        // a 시간이 바뀌고 b가 사라지고 c가 새로 생김
        List<ExternalEvent> second = List.of(ext("a", "출근", NINE.plusHours(1)), ext("c", "장보기", NINE));
        assertThat(importer.apply(EventSource.NOTION, second, false)).isEqualTo(new EventImporter.Counts(1, 1, 1, 0));
        assertThat(find(EventSource.NOTION, "a").getStartAt()).isEqualTo(NINE.plusHours(1));
        assertThat(events.findBySource(EventSource.NOTION)).extracting(Event::getExternalId).containsExactlyInAnyOrder("a", "c");
    }

    @Test
    void manualAndOtherSourceEventsAreNeverTouched() {
        Event manual = events.save(new Event("출근", NINE, null, null, "직접 적은 메모", false));
        Event icloud = events.save(Event.imported(EventSource.ICLOUD, "uid-1", "아이클라우드", NINE, null, null, false));

        importer.apply(EventSource.NOTION, List.of(ext("a", "출근", NINE)), false);
        importer.apply(EventSource.NOTION, List.of(ext("z", "다른 것", NINE)), false);

        assertThat(events.findById(manual.getId())).get()
                .satisfies(e -> {
                    assertThat(e.getSource()).isEqualTo(EventSource.MANUAL);
                    assertThat(e.getExternalId()).isNull();
                    assertThat(e.getDescription()).isEqualTo("직접 적은 메모");
                });
        assertThat(events.findById(icloud.getId())).isPresent();
    }

    @Test
    void updateKeepsDescriptionWrittenInHubDash() {
        importer.apply(EventSource.NOTION, List.of(ext("a", "출근", NINE)), false);
        Event e = find(EventSource.NOTION, "a");
        e.update(e.getTitle(), e.getStartAt(), e.getEndAt(), e.getLocation(), "HubDash에서 적은 설명", e.getAllDay());
        events.flush();

        importer.apply(EventSource.NOTION, List.of(ext("a", "출근 (변경)", NINE)), false);

        assertThat(find(EventSource.NOTION, "a").getDescription()).isEqualTo("HubDash에서 적은 설명");
        assertThat(find(EventSource.NOTION, "a").getTitle()).isEqualTo("출근 (변경)");
    }

    @Test
    void emptyFetchDoesNotWipeExistingEvents() {
        importer.apply(EventSource.NOTION, List.of(ext("a", "출근", NINE)), false);

        assertThatThrownBy(() -> importer.apply(EventSource.NOTION, List.of(), false))
                .hasMessageContaining("0건");
        assertThat(events.findBySource(EventSource.NOTION)).hasSize(1);
    }

    @Test
    void sameExternalIdTwiceBecomesOneEvent() {
        importer.apply(EventSource.ICLOUD, List.of(ext("u", "A", NINE), ext("u", "B", NINE)), false);
        assertThat(events.findBySource(EventSource.ICLOUD)).singleElement()
                .extracting(Event::getTitle).isEqualTo("B");
    }

    @Test
    void backfillAdoptsMigratedEventsOneToOneAndLeavesTheRestManual() {
        // 노션에서 이관한 일정(출처 없음) — 같은 제목·시각이 두 번 있는 날도 있다
        Event op6a = events.save(new Event("OP6 출근", NINE, NINE.plusHours(1), null, null, false));
        Event op6b = events.save(new Event("OP6 출근", NINE, NINE.plusHours(1), null, null, false));
        Event mine = events.save(new Event("직접 만든 일정", NINE, null, null, null, false));

        EventImporter.Counts c = importer.apply(EventSource.NOTION, List.of(
                ext("p1", "OP6 출근", NINE),
                ext("p2", " OP6 출근 ", NINE),
                ext("p3", "노션에만 있는 일정", NINE)), true);

        // 두 이관 일정은 원본과 짝지어져(추가 아님) 끝 시각만 원본대로 갱신, 새 일정 하나만 추가
        assertThat(c).isEqualTo(new EventImporter.Counts(1, 2, 0, 2));
        assertThat(events.findById(op6a.getId())).get().extracting(Event::getSource).isEqualTo(EventSource.NOTION);
        assertThat(events.findById(op6b.getId())).get().extracting(Event::getSource).isEqualTo(EventSource.NOTION);
        assertThat(events.findById(op6a.getId()).get().getEndAt()).isNull();
        assertThat(events.findById(mine.getId())).get().extracting(Event::getSource).isEqualTo(EventSource.MANUAL);
        assertThat(events.count()).isEqualTo(4);

        // 다시 돌려도(백필 없이) 중복 없음
        assertThat(importer.apply(EventSource.NOTION, List.of(ext("p1", "OP6 출근", NINE), ext("p2", " OP6 출근 ", NINE),
                ext("p3", "노션에만 있는 일정", NINE)), false)).isEqualTo(new EventImporter.Counts(0, 0, 0, 0));
    }
}
