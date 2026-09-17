package com.junyoung.dashboard.domain.memo.consumer;

import com.junyoung.dashboard.domain.memo.entity.Memo;
import com.junyoung.dashboard.domain.memo.entity.RawMemo;
import com.junyoung.dashboard.domain.memo.entity.RawStatus;
import com.junyoung.dashboard.domain.memo.event.RawMemoCreatedEvent;
import com.junyoung.dashboard.domain.memo.repository.MemoRepository;
import com.junyoung.dashboard.domain.memo.repository.RawMemoRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 검증 실패는 "일시적 장애"가 아니라 "비즈니스 실패"다 — 예외를 던져 Kafka가 무한 재시도/재배달하게 만들지 않고,
// RawMemo.status를 FAILED로 명시적으로 기록한 뒤 정상 종료(ack)한다.
@Component
public class RawMemoConsumer {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 5000;

    private static final Logger log = LoggerFactory.getLogger(RawMemoConsumer.class);

    private final RawMemoRepository rawMemoRepository;
    private final MemoRepository memoRepository;

    public RawMemoConsumer(RawMemoRepository rawMemoRepository, MemoRepository memoRepository) {
        this.rawMemoRepository = rawMemoRepository;
        this.memoRepository = memoRepository;
    }

    @KafkaListener(topics = KafkaTopicConfig.MEMO_RAW_TOPIC)
    @Transactional
    public void consume(RawMemoCreatedEvent event) {
        RawMemo raw = rawMemoRepository.findById(event.rawMemoId()).orElse(null);
        if (raw == null) {
            log.warn("RawMemo {} not found, skipping", event.rawMemoId());
            return;
        }
        // Kafka는 at-least-once 전달을 보장한다 — 크래시/리밸런싱으로 같은 이벤트가 재전달될 수 있으므로,
        // 이미 처리 끝난(PROCESSED/FAILED) 레코드는 중복 처리(Memo 중복 생성)하지 않도록 건너뛴다.
        if (raw.getStatus() != RawStatus.PENDING) {
            log.info("RawMemo {} already in status {}, skipping duplicate delivery", raw.getId(), raw.getStatus());
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

        String contentRaw = raw.getContentRaw();
        if (contentRaw == null || contentRaw.isBlank()) {
            raw.markFailed("contentRaw가 비어있음");
            return;
        }
        if (contentRaw.length() > CONTENT_MAX_LENGTH) {
            raw.markFailed("contentRaw가 " + CONTENT_MAX_LENGTH + "자를 초과함: " + contentRaw.length() + "자");
            return;
        }

        memoRepository.save(new Memo(titleRaw, contentRaw, raw.getTags()));
        raw.markProcessed();
    }
}
