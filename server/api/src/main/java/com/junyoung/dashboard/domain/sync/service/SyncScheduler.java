package com.junyoung.dashboard.domain.sync.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 30분마다 일정 동기화 (이슈 #228). 서버가 상시 켜져 있으므로(윈도우 PC Docker) 주기 실행으로 충분하다.
 * app.sync.enabled=false로 끌 수 있다(테스트 프로파일은 끔).
 */
@Component
public class SyncScheduler {

    private final ScheduleSyncService service;
    private final boolean enabled;

    public SyncScheduler(ScheduleSyncService service, @Value("${app.sync.enabled:true}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    @Scheduled(initialDelayString = "${app.sync.initial-delay:PT2M}", fixedDelayString = "${app.sync.interval:PT30M}")
    public void run() {
        if (enabled) {
            service.syncIfIdle();
        }
    }
}
