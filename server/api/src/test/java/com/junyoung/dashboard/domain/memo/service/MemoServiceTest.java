package com.junyoung.dashboard.domain.memo.service;

import com.junyoung.dashboard.domain.memo.dto.MemoRequest;
import com.junyoung.dashboard.domain.memo.dto.MemoResponse;
import com.junyoung.dashboard.domain.memo.entity.Memo;
import com.junyoung.dashboard.domain.memo.repository.MemoRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

    @Mock
    private MemoRepository memoRepository;

    private MemoService memoService;

    @BeforeEach
    void setUp() {
        memoService = new MemoService(memoRepository);
    }

    @Test
    void createsMemoUsingRequestFields() {
        MemoRequest request = new MemoRequest("장보기", "우유, 계란, 빵", "일상,장보기");
        when(memoRepository.save(any(Memo.class)))
                .thenReturn(new Memo("장보기", "우유, 계란, 빵", "일상,장보기"));

        MemoResponse response = memoService.create(request);

        assertThat(response.title()).isEqualTo("장보기");

        ArgumentCaptor<Memo> captor = ArgumentCaptor.forClass(Memo.class);
        verify(memoRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo(request.content());
        assertThat(captor.getValue().getTags()).isEqualTo(request.tags());
    }

    @Test
    void throwsWhenMemoNotFound() {
        when(memoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updatesEveryFieldWithoutTransposingParameters() {
        Memo existing = new Memo("기존 제목", "기존 본문", "기존태그");
        when(memoRepository.findById(1L)).thenReturn(Optional.of(existing));

        MemoRequest request = new MemoRequest("새 제목", "새 본문", "새태그");

        MemoResponse response = memoService.update(1L, request);

        assertThat(response.title()).isEqualTo("새 제목");
        assertThat(response.content()).isEqualTo("새 본문");
        assertThat(response.tags()).isEqualTo("새태그");
    }
}
