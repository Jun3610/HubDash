package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.RawHealthLogRequest;
import com.junyoung.dashboard.domain.health.dto.RawHealthLogResponse;
import com.junyoung.dashboard.domain.health.entity.RawHealthLog;
import com.junyoung.dashboard.domain.health.event.RawHealthLogCreatedEvent;
import com.junyoung.dashboard.domain.health.repository.RawHealthLogRepository;
import com.junyoung.dashboard.global.config.KafkaTopicConfig;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RawHealthLogServiceTest {

    @Mock
    private RawHealthLogRepository rawHealthLogRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private RawHealthLogService rawHealthLogService;

    @BeforeEach
    void setUp() {
        rawHealthLogService = new RawHealthLogService(rawHealthLogRepository, kafkaTemplate);
    }

    @Test
    void createsRawHealthLogAndPublishesEventWithSavedId() {
        RawHealthLogRequest request = new RawHealthLogRequest("2026-09-17", "70.5", 7.5, null);
        RawHealthLog saved = new RawHealthLog("2026-09-17", "70.5", 7.5, null);
        ReflectionTestUtils.setField(saved, "id", 42L);
        when(rawHealthLogRepository.save(any(RawHealthLog.class))).thenReturn(saved);

        RawHealthLogResponse response = rawHealthLogService.create(request);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.status()).isEqualTo("PENDING");

        ArgumentCaptor<RawHealthLogCreatedEvent> eventCaptor = ArgumentCaptor.forClass(RawHealthLogCreatedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopicConfig.HEALTH_RAW_LOG_TOPIC), eventCaptor.capture());
        assertThat(eventCaptor.getValue().rawHealthLogId()).isEqualTo(42L);
    }

    @Test
    void throwsWhenRawHealthLogNotFound() {
        when(rawHealthLogRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rawHealthLogService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }
}
