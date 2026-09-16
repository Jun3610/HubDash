package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicResponse;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
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
class StudyTopicServiceTest {

    @Mock
    private StudyTopicRepository studyTopicRepository;

    private StudyTopicService studyTopicService;

    @BeforeEach
    void setUp() {
        studyTopicService = new StudyTopicService(studyTopicRepository);
    }

    @Test
    void createsTopicUsingRequestFields() {
        StudyTopicRequest request = new StudyTopicRequest("토익", "영어 공부");
        when(studyTopicRepository.save(any(StudyTopic.class)))
                .thenReturn(new StudyTopic("토익", "영어 공부"));

        StudyTopicResponse response = studyTopicService.create(request);

        assertThat(response.name()).isEqualTo("토익");

        ArgumentCaptor<StudyTopic> captor = ArgumentCaptor.forClass(StudyTopic.class);
        verify(studyTopicRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo(request.name());
        assertThat(captor.getValue().getDescription()).isEqualTo(request.description());
    }

    @Test
    void throwsWhenTopicNotFound() {
        when(studyTopicRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyTopicService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }
}
