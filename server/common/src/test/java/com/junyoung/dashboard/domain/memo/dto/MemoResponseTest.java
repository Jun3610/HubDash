package com.junyoung.dashboard.domain.memo.dto;

import com.junyoung.dashboard.domain.memo.entity.Memo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        Memo memo = new Memo("장보기", "우유, 계란, 빵", "일상,장보기");

        MemoResponse response = MemoResponse.from(memo);

        assertThat(response.title()).isEqualTo("장보기");
        assertThat(response.content()).isEqualTo("우유, 계란, 빵");
        assertThat(response.tags()).isEqualTo("일상,장보기");
    }
}
