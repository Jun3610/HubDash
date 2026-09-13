package com.junyoung.dashboard.global.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successWrapsDataAndClearsError() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getMessage()).isNull();
    }

    @Test
    void errorWrapsCodeAndMessageWithNullData() {
        ApiResponse<String> response = ApiResponse.error("NOT_FOUND", "찾을 수 없음");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("찾을 수 없음");
    }
}
