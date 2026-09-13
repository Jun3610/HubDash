package com.junyoung.dashboard.global.common;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Entity
    static class SampleEntity extends BaseEntity {
        SampleEntity() {
        }
    }

    @Test
    void hasNullIdAndTimestampsBeforePersistence() {
        SampleEntity entity = new SampleEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }
}
