package com.junyoung.dashboard.domain.hub.consumer;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.domain.hub.entity.RawHubLink;
import com.junyoung.dashboard.domain.hub.entity.RawStatus;
import com.junyoung.dashboard.domain.hub.event.RawHubLinkCreatedEvent;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
import com.junyoung.dashboard.domain.hub.repository.RawHubLinkRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 검증 실패는 "일시적 장애"가 아니라 "비즈니스 실패"다 — 예외를 던져 Kafka가 무한 재시도/재배달하게 만들지 않고,
// RawHubLink.status를 FAILED로 명시적으로 기록한 뒤 정상 종료(ack)한다.
// 부모(HubCategory) 존재 확인도 같은 원칙 — EntityNotFoundException을 던지지 않고 findById 결과를 직접 체크한다.
@Component
public class RawHubLinkConsumer {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int URL_MAX_LENGTH = 1000;

    private static final Logger log = LoggerFactory.getLogger(RawHubLinkConsumer.class);

    private final RawHubLinkRepository rawHubLinkRepository;
    private final HubLinkRepository hubLinkRepository;
    private final HubCategoryRepository hubCategoryRepository;

    public RawHubLinkConsumer(RawHubLinkRepository rawHubLinkRepository,
                               HubLinkRepository hubLinkRepository,
                               HubCategoryRepository hubCategoryRepository) {
        this.rawHubLinkRepository = rawHubLinkRepository;
        this.hubLinkRepository = hubLinkRepository;
        this.hubCategoryRepository = hubCategoryRepository;
    }

    @KafkaListener(topics = KafkaTopicConfig.HUB_RAW_LINK_TOPIC)
    @Transactional
    public void consume(RawHubLinkCreatedEvent event) {
        RawHubLink raw = rawHubLinkRepository.findById(event.rawHubLinkId()).orElse(null);
        if (raw == null) {
            log.warn("RawHubLink {} not found, skipping", event.rawHubLinkId());
            return;
        }
        // Kafka는 at-least-once 전달을 보장한다 — 크래시/리밸런싱으로 같은 이벤트가 재전달될 수 있으므로,
        // 이미 처리 끝난(PROCESSED/FAILED) 레코드는 중복 처리(HubLink 중복 생성)하지 않도록 건너뛴다.
        if (raw.getStatus() != RawStatus.PENDING) {
            log.info("RawHubLink {} already in status {}, skipping duplicate delivery", raw.getId(), raw.getStatus());
            return;
        }

        Long categoryId;
        try {
            categoryId = Long.parseLong(raw.getCategoryIdRaw());
        } catch (NumberFormatException e) {
            raw.markFailed("categoryIdRaw 파싱 실패: " + raw.getCategoryIdRaw());
            return;
        }

        HubCategory category = hubCategoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            raw.markFailed("categoryId " + categoryId + " not found");
            return;
        }

        String titleRaw = raw.getTitleRaw();
        if (titleRaw == null || titleRaw.isBlank()) {
            raw.markFailed("titleRaw가 비어있음");
            return;
        }
        if (titleRaw.length() > TITLE_MAX_LENGTH) {
            raw.markFailed("titleRaw가 " + TITLE_MAX_LENGTH + "자를 초과함: " + titleRaw.length() + "자");
            return;
        }

        String urlRaw = raw.getUrlRaw();
        if (urlRaw == null || urlRaw.isBlank()) {
            raw.markFailed("urlRaw가 비어있음");
            return;
        }
        if (urlRaw.length() > URL_MAX_LENGTH) {
            raw.markFailed("urlRaw가 " + URL_MAX_LENGTH + "자를 초과함: " + urlRaw.length() + "자");
            return;
        }

        HubLink saved = new HubLink(category, titleRaw, urlRaw, raw.getDescription());
        hubLinkRepository.save(saved);
        raw.markProcessed();
    }
}
