package com.junyoung.dashboard.domain.pknu.dto;

import com.junyoung.dashboard.domain.pknu.entity.Semester;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SemesterResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        Semester semester = new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));

        SemesterResponse response = SemesterResponse.from(semester);

        assertThat(response.name()).isEqualTo("2026-1학기");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }
}
