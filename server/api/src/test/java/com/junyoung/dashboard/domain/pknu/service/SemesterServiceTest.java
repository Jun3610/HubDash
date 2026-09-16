package com.junyoung.dashboard.domain.pknu.service;

import com.junyoung.dashboard.domain.pknu.dto.SemesterRequest;
import com.junyoung.dashboard.domain.pknu.dto.SemesterResponse;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.domain.pknu.repository.SemesterRepository;
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
class SemesterServiceTest {

    @Mock
    private SemesterRepository semesterRepository;

    private SemesterService semesterService;

    @BeforeEach
    void setUp() {
        semesterService = new SemesterService(semesterRepository);
    }

    @Test
    void createsSemesterUsingRequestFields() {
        SemesterRequest request = new SemesterRequest("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
        when(semesterRepository.save(any(Semester.class)))
                .thenReturn(new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30)));

        SemesterResponse response = semesterService.create(request);

        assertThat(response.name()).isEqualTo("2026-1학기");

        ArgumentCaptor<Semester> captor = ArgumentCaptor.forClass(Semester.class);
        verify(semesterRepository).save(captor.capture());
        assertThat(captor.getValue().getStartDate()).isEqualTo(request.startDate());
        assertThat(captor.getValue().getEndDate()).isEqualTo(request.endDate());
    }

    @Test
    void throwsWhenSemesterNotFound() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semesterService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updatesEveryFieldWithoutTransposingParameters() {
        Semester existing = new Semester("2025-2학기", LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 20));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(existing));

        SemesterRequest request = new SemesterRequest("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));

        SemesterResponse response = semesterService.update(1L, request);

        assertThat(response.name()).isEqualTo("2026-1학기");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }
}
