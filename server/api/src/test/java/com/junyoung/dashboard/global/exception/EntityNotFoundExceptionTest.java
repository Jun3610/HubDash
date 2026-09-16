package com.junyoung.dashboard.global.exception;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import com.junyoung.dashboard.domain.memo.entity.Memo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityNotFoundExceptionTest {

    @Test
    void ofSingleWordEntityProducesLowercaseMessage() {
        EntityNotFoundException ex = EntityNotFoundException.of(Memo.class, 1L);

        assertThat(ex.getMessage()).isEqualTo("memo 1 not found");
    }

    @Test
    void ofCompoundNameEntitySplitsWordsWithSpaces() {
        EntityNotFoundException ex = EntityNotFoundException.of(HubCategory.class, 2L);

        assertThat(ex.getMessage()).isEqualTo("hub category 2 not found");
    }

    @Test
    void ofEntityNameEndingInLogSplitsCorrectly() {
        EntityNotFoundException ex = EntityNotFoundException.of(HabitLog.class, 3L);

        assertThat(ex.getMessage()).isEqualTo("habit log 3 not found");
    }

    @Test
    void ofReadingLogSplitsCorrectly() {
        EntityNotFoundException ex = EntityNotFoundException.of(ReadingLog.class, 4L);

        assertThat(ex.getMessage()).isEqualTo("reading log 4 not found");
    }

    @Test
    void ofHubLinkSplitsCorrectly() {
        EntityNotFoundException ex = EntityNotFoundException.of(HubLink.class, 5L);

        assertThat(ex.getMessage()).isEqualTo("hub link 5 not found");
    }
}
