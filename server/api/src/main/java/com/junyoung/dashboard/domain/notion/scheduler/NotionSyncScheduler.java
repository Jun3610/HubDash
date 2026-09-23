package com.junyoung.dashboard.domain.notion.scheduler;

import com.junyoung.dashboard.domain.notion.service.NotionSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 서버를 필요할 때만 켜서 쓰는 운영 방식이라(#93), 기동 직후 첫 동기화가 사실상 주된 반영 시점이다.
// 첫 실행은 기동 후 initial-delay만큼 기다려 Kafka 연결이 잡힌 뒤에 돈다.
@Component
public class NotionSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotionSyncScheduler.class);

    private final NotionSyncService notionSyncService;

    public NotionSyncScheduler(NotionSyncService notionSyncService) {
        this.notionSyncService = notionSyncService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStatus() {
        if (!notionSyncService.isEnabled()) {
            log.info("NOTION_TOKEN이 없어 노션 동기화를 건너뜁니다");
        }
    }

    @Scheduled(initialDelayString = "${app.notion.sync-initial-delay:PT30S}",
            fixedDelayString = "${app.notion.sync-interval:PT10M}")
    public void sync() {
        if (!notionSyncService.isEnabled()) {
            return;
        }
        notionSyncService.syncAll();
    }
}
