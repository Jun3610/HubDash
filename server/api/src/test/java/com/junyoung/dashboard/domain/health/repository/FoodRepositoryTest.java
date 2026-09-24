package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.Food;
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
class FoodRepositoryTest {

    @Autowired
    private FoodRepository foodRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        Food saved = foodRepository.save(
                new Food("바나나", 100, 27.0, 0.3, 1.3, "1개", true));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
