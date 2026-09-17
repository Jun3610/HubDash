package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.dto.RawHubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.RawHubLinkResponse;
import com.junyoung.dashboard.domain.hub.entity.RawHubLink;
import com.junyoung.dashboard.domain.hub.event.RawHubLinkCreatedEvent;
import com.junyoung.dashboard.domain.hub.repository.RawHubLinkRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RawHubLinkService {

    private final RawHubLinkRepository rawHubLinkRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RawHubLinkService(RawHubLinkRepository rawHubLinkRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.rawHubLinkRepository = rawHubLinkRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public RawHubLinkResponse create(RawHubLinkRequest request) {
        RawHubLink saved = rawHubLinkRepository.save(new RawHubLink(
                request.categoryIdRaw(),
                request.titleRaw(),
                request.urlRaw(),
                request.description()
        ));
        kafkaTemplate.send(KafkaTopicConfig.HUB_RAW_LINK_TOPIC, new RawHubLinkCreatedEvent(saved.getId()));
        return RawHubLinkResponse.from(saved);
    }

    public RawHubLinkResponse findById(Long id) {
        return RawHubLinkResponse.from(getOrThrow(id));
    }

    private RawHubLink getOrThrow(Long id) {
        return rawHubLinkRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(RawHubLink.class, id));
    }
}
