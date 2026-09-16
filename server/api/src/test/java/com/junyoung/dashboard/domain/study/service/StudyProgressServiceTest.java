package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressResponse;
import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.repository.StudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
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
class StudyProgressServiceTest {

    @Mock
    private StudyProgressRepository studyProgressRepository;

    @Mock
    private StudyTopicRepository studyTopicRepository;

    private StudyProgressService studyProgressService;

    @BeforeEach
    void setUp() {
        studyProgressService = new StudyProgressService(studyProgressRepository, studyTopicRepository);
    }

    @Test
    void createsProgressUnderExistingTopic() {
        StudyTopic topic = new StudyTopic("토익", null);
        StudyProgressRequest request = new StudyProgressRequest(1L, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이");
        when(studyTopicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(studyProgressRepository.save(any(StudyProgress.class)))
                .thenReturn(new StudyProgress(topic, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이"));

        StudyProgressResponse response = studyProgressService.create(request);

        assertThat(response.minutes()).isEqualTo(60);

        ArgumentCaptor<StudyProgress> captor = ArgumentCaptor.forClass(StudyProgress.class);
        verify(studyProgressRepository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isSameAs(topic);
    }

    @Test
    void throwsWhenTopicMissingOnCreate() {
        StudyProgressRequest request = new StudyProgressRequest(1L, LocalDate.of(2026, 9, 14), 60, null);
        when(studyTopicRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyProgressService.create(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void movesProgressToNewTopicOnUpdate() {
        StudyTopic oldTopic = new StudyTopic("토익", null);
        StudyTopic newTopic = new StudyTopic("알고리즘", null);
        StudyProgress progress = new StudyProgress(oldTopic, LocalDate.of(2026, 9, 14), 60, null);
        StudyProgressRequest request = new StudyProgressRequest(2L, LocalDate.of(2026, 9, 15), 90, "DP 복습");
        when(studyProgressRepository.findById(10L)).thenReturn(Optional.of(progress));
        when(studyTopicRepository.findById(2L)).thenReturn(Optional.of(newTopic));

        studyProgressService.update(10L, request);

        assertThat(progress.getTopic()).isSameAs(newTopic);
        assertThat(progress.getTopic()).isNotSameAs(oldTopic);
        assertThat(progress.getMinutes()).isEqualTo(90);
    }

    @Test
    void throwsWhenNewTopicMissingOnUpdate() {
        StudyTopic oldTopic = new StudyTopic("토익", null);
        StudyProgress progress = new StudyProgress(oldTopic, LocalDate.of(2026, 9, 14), 60, null);
        StudyProgressRequest request = new StudyProgressRequest(2L, LocalDate.of(2026, 9, 15), 90, null);
        when(studyProgressRepository.findById(10L)).thenReturn(Optional.of(progress));
        when(studyTopicRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyProgressService.update(10L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
