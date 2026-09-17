package com.junyoung.dashboard.domain.memo.service;

import com.junyoung.dashboard.domain.memo.dto.RawMemoRequest;
import com.junyoung.dashboard.domain.memo.dto.RawMemoResponse;
import com.junyoung.dashboard.domain.memo.entity.RawMemo;
import com.junyoung.dashboard.domain.memo.event.RawMemoCreatedEvent;
import com.junyoung.dashboard.domain.memo.repository.RawMemoRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RawMemoService {

    private final RawMemoRepository rawMemoRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RawMemoService(RawMemoRepository rawMemoRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.rawMemoRepository = rawMemoRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public RawMemoResponse create(RawMemoRequest request) {
        RawMemo saved = rawMemoRepository.save(new RawMemo(request.titleRaw(), request.contentRaw(), request.tags()));
        kafkaTemplate.send(KafkaTopicConfig.MEMO_RAW_TOPIC, new RawMemoCreatedEvent(saved.getId()));
        return RawMemoResponse.from(saved);
    }

    public RawMemoResponse findById(Long id) {
        return RawMemoResponse.from(getOrThrow(id));
    }

    private RawMemo getOrThrow(Long id) {
        return rawMemoRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(RawMemo.class, id));
    }
}
