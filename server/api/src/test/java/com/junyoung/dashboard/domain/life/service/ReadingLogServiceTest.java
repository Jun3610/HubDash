package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.ReadingLogRequest;
import com.junyoung.dashboard.domain.life.dto.ReadingLogResponse;
import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import com.junyoung.dashboard.domain.life.repository.ReadingLogRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingLogServiceTest {

    @Mock
    private ReadingLogRepository readingLogRepository;

    private ReadingLogService readingLogService;

    @BeforeEach
    void setUp() {
        readingLogService = new ReadingLogService(readingLogRepository);
    }

    @Test
    void createsReadingLogUsingRequestFields() {
        ReadingLogRequest request = new ReadingLogRequest("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, null, null);
        when(readingLogRepository.save(any(ReadingLog.class)))
                .thenReturn(new ReadingLog("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, null, null));

        ReadingLogResponse response = readingLogService.create(request);

        assertThat(response.title()).isEqualTo("클린 코드");

        ArgumentCaptor<ReadingLog> captor = ArgumentCaptor.forClass(ReadingLog.class);
        verify(readingLogRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo(request.title());
        assertThat(captor.getValue().getAuthor()).isEqualTo(request.author());
    }

    @Test
    void throwsWhenReadingLogNotFound() {
        when(readingLogRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readingLogService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }
}
