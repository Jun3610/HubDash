package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReadingLogResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        ReadingLog log = new ReadingLog("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, 5, "좋았음");

        ReadingLogResponse response = ReadingLogResponse.from(log);

        assertThat(response.title()).isEqualTo("클린 코드");
        assertThat(response.author()).isEqualTo("로버트 마틴");
        assertThat(response.rating()).isEqualTo(5);
    }
}
