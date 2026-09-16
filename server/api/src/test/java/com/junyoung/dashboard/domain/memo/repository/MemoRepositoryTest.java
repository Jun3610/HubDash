package com.junyoung.dashboard.domain.memo.repository;

import com.junyoung.dashboard.domain.memo.entity.Memo;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class MemoRepositoryTest {

    @Autowired
    private MemoRepository memoRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        Memo saved = memoRepository.save(new Memo("장보기", "우유, 계란, 빵", "일상,장보기"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
