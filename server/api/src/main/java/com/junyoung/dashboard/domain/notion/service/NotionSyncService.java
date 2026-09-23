package com.junyoung.dashboard.domain.notion.service;

import com.junyoung.dashboard.domain.notion.client.NotionClient;
import com.junyoung.dashboard.domain.notion.dto.NotionSyncResult;
import com.junyoung.dashboard.domain.notion.entity.NotionSyncSource;
import com.junyoung.dashboard.domain.notion.repository.NotionSyncSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

// 등록된 소스 전부를 차례로 동기화한다. 한 소스의 실패(노션 API 오류, 통합이 연결 안 된 페이지 등)는
// 결과에 기록하고 다음 소스로 넘어간다.
@Service
public class NotionSyncService {

    private static final Logger log = LoggerFactory.getLogger(NotionSyncService.class);

    private final NotionClient notionClient;
    private final NotionSyncSourceRepository sourceRepository;
    private final NotionSourceSynchronizer synchronizer;
    // 주기 실행과 수동 실행이 겹치면 같은 페이지를 두 번 넣을 수 있어 한 번에 하나만 돌린다.
    private final ReentrantLock lock = new ReentrantLock();

    public NotionSyncService(NotionClient notionClient,
                             NotionSyncSourceRepository sourceRepository,
                             NotionSourceSynchronizer synchronizer) {
        this.notionClient = notionClient;
        this.sourceRepository = sourceRepository;
        this.synchronizer = synchronizer;
    }

    public boolean isEnabled() {
        return notionClient.isConfigured();
    }

    public List<NotionSyncResult> syncAll() {
        lock.lock();
        try {
            List<NotionSyncResult> results = new ArrayList<>();
            for (NotionSyncSource source : sourceRepository.findAll()) {
                results.add(syncOne(source));
            }
            return results;
        } finally {
            lock.unlock();
        }
    }

    private NotionSyncResult syncOne(NotionSyncSource source) {
        try {
            NotionSyncResult result = synchronizer.sync(source.getId());
            if (result.created() > 0) {
                log.info("Notion source {} synced: {} new link(s)", source.getNotionId(), result.created());
            }
            return result;
        } catch (RuntimeException e) {
            log.warn("Notion source {} sync failed: {}", source.getNotionId(), e.getMessage());
            return NotionSyncResult.failure(source.getId(), source.getNotionId(), e.getMessage());
        }
    }
}
