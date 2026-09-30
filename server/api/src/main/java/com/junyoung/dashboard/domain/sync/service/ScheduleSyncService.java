package com.junyoung.dashboard.domain.sync.service;

import com.junyoung.dashboard.domain.hub.service.HubNotionSyncService;
import com.junyoung.dashboard.domain.schedule.entity.EventSource;
import com.junyoung.dashboard.domain.sync.dto.SyncResponse;
import com.junyoung.dashboard.domain.sync.dto.SyncSourceResult;
import com.junyoung.dashboard.domain.sync.dto.SyncStatusResponse;
import com.junyoung.dashboard.domain.sync.entity.SyncRun;
import com.junyoung.dashboard.domain.sync.repository.SyncRunRepository;
import com.junyoung.dashboard.domain.sync.source.ExternalEvent;
import com.junyoung.dashboard.domain.sync.source.SyncSource;
import com.junyoung.dashboard.global.exception.IntegrationException;
import com.junyoung.dashboard.global.notion.NotionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 웹의 "갱신" 버튼과 30분마다 도는 동기화 (이슈 #228).
 * 연결된 소스마다 가져와 반영하고, 소스 하나가 실패해도 나머지는 계속 돈다. 결과는 sync_run에 남긴다.
 * 허브 링크의 노션 불러오기(#163)도 같이 돌린다(NOTION_HUB, 새 글 추가만).
 */
@Service
public class ScheduleSyncService {

    static final String HUB = "NOTION_HUB";
    private static final Logger log = LoggerFactory.getLogger(ScheduleSyncService.class);

    private final List<SyncSource> sources;
    private final EventImporter importer;
    private final SyncRunRepository runs;
    private final HubNotionSyncService hubSync;
    private final NotionClient notion;
    private final Clock clock;
    // 버튼과 주기 실행이 겹치면 같은 원본을 두 번 넣을 수 있어 한 번에 하나만
    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    public ScheduleSyncService(List<SyncSource> sources, EventImporter importer, SyncRunRepository runs,
                               HubNotionSyncService hubSync, NotionClient notion) {
        this(sources, importer, runs, hubSync, notion, Clock.systemDefaultZone());
    }

    ScheduleSyncService(List<SyncSource> sources, EventImporter importer, SyncRunRepository runs,
                        HubNotionSyncService hubSync, NotionClient notion, Clock clock) {
        this.sources = sources;
        this.importer = importer;
        this.runs = runs;
        this.hubSync = hubSync;
        this.notion = notion;
        this.clock = clock;
    }

    /** 수동 실행 — 이미 돌고 있으면 409 */
    public SyncResponse syncAll() {
        if (!lock.tryLock()) {
            throw new IntegrationException(HttpStatus.CONFLICT, "SYNC_RUNNING", "이미 동기화 중이에요 — 잠시 후 다시 눌러 주세요");
        }
        try {
            List<SyncSourceResult> results = new ArrayList<>();
            for (SyncSource source : sources) {
                if (source.configured()) {
                    results.add(runOne(source));
                }
            }
            if (notion.configured()) {
                results.add(runHub());
            }
            return new SyncResponse(results, now());
        } finally {
            lock.unlock();
        }
    }

    /** 주기 실행 — 수동 실행 중이면 이번 차례는 건너뛴다 */
    public void syncIfIdle() {
        if (lock.isLocked()) {
            return;
        }
        try {
            SyncResponse response = syncAll();
            response.sources().stream().filter(r -> !r.ok())
                    .forEach(r -> log.warn("동기화 실패 {}: {}", r.name(), r.error()));
        } catch (IntegrationException busy) {
            // 방금 수동 실행이 시작됨
        }
    }

    public SyncStatusResponse status() {
        List<SyncStatusResponse.SourceStatus> list = new ArrayList<>();
        for (SyncSource source : sources) {
            list.add(new SyncStatusResponse.SourceStatus(source.name(), source.configured(), lastRun(source.name())));
        }
        list.add(new SyncStatusResponse.SourceStatus(HUB, notion.configured(), lastRun(HUB)));
        return new SyncStatusResponse(list, lock.isLocked());
    }

    private SyncSourceResult runOne(SyncSource source) {
        LocalDateTime started = now();
        try {
            List<ExternalEvent> fetched = source.fetch();
            // 직접 만든 일정과의 짝짓기(백필):
            // - 노션(#230): 이관 일정 321건의 출처 채우기 — 노션 첫 동기화(성공 기록이 없을 때) 한 번만
            // - iCloud(#234): iCloud가 일정 원본이라 노션 일정을 옮겨 둔 캘린더도 켠다 — 캘린더를 나중에 더 켜도 중복되지 않게 매번
            boolean backfill = source.source() == EventSource.ICLOUD
                    || source.source() == EventSource.NOTION && !runs.existsBySourceAndOkTrue(source.name());
            EventImporter.Counts c = importer.apply(source.source(), fetched, backfill);
            return record(source.name(), started, new SyncSourceResult(source.name(), true, c.added(), c.updated(), c.deleted(), null));
        } catch (RuntimeException e) {
            return record(source.name(), started, SyncSourceResult.failed(source.name(), message(e)));
        }
    }

    private SyncSourceResult runHub() {
        LocalDateTime started = now();
        try {
            HubNotionSyncService.SyncResult r = hubSync.sync();
            return record(HUB, started, new SyncSourceResult(HUB, true, r.added(), 0, 0, null));
        } catch (RuntimeException e) {
            return record(HUB, started, SyncSourceResult.failed(HUB, message(e)));
        }
    }

    private SyncSourceResult record(String name, LocalDateTime started, SyncSourceResult r) {
        runs.save(new SyncRun(name, started, now(), r.ok(), r.added(), r.updated(), r.deleted(), r.error()));
        return r;
    }

    private SyncStatusResponse.LastRun lastRun(String name) {
        return runs.findFirstBySourceOrderByStartedAtDescIdDesc(name)
                .map(r -> new SyncStatusResponse.LastRun(r.getStartedAt(), r.getFinishedAt(), r.isOk(),
                        r.getAdded(), r.getUpdated(), r.getDeleted(), r.getError()))
                .orElse(null);
    }

    // 예상 못 한 예외는 원인을 로그에만 남기고 화면에는 짧게
    private static String message(RuntimeException e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        log.error("동기화 중 예외", e);
        return "동기화 중 오류가 났어요 (" + e.getClass().getSimpleName() + ")";
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
