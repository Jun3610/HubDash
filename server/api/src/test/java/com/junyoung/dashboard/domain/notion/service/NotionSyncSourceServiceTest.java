package com.junyoung.dashboard.domain.notion.service;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.notion.dto.NotionSyncSourceRequest;
import com.junyoung.dashboard.domain.notion.entity.NotionSourceType;
import com.junyoung.dashboard.domain.notion.entity.NotionSyncSource;
import com.junyoung.dashboard.domain.notion.repository.NotionSyncSourceRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import com.junyoung.dashboard.global.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotionSyncSourceServiceTest {

    private static final String ID = "66d2b9e1fd1482abb73a01f2f39c987c";

    @Mock
    private NotionSyncSourceRepository sourceRepository;
    @Mock
    private HubCategoryRepository hubCategoryRepository;
    @InjectMocks
    private NotionSyncSourceService service;

    @Test
    void registersSourceFromNotionUrlWithNormalizedId() {
        HubCategory category = new HubCategory("Docker", null);
        when(hubCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(sourceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new NotionSyncSourceRequest(
                "https://app.notion.com/p/" + ID + "?pvs=204", NotionSourceType.DATABASE, 1L));

        ArgumentCaptor<NotionSyncSource> saved = ArgumentCaptor.forClass(NotionSyncSource.class);
        verify(sourceRepository).save(saved.capture());
        assertThat(saved.getValue().getNotionId()).isEqualTo(ID);
        assertThat(saved.getValue().getHubCategory()).isSameAs(category);
    }

    @Test
    void rejectsValueWithoutNotionId() {
        assertThatThrownBy(() -> service.create(
                new NotionSyncSourceRequest("https://example.com", NotionSourceType.PAGE, 1L)))
                .isInstanceOf(InvalidRequestException.class);
        verify(sourceRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateSource() {
        when(sourceRepository.existsByNotionId(ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new NotionSyncSourceRequest(ID, NotionSourceType.DATABASE, 1L)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("이미 등록된");
    }

    @Test
    void rejectsMissingCategory() {
        when(hubCategoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new NotionSyncSourceRequest(ID, NotionSourceType.DATABASE, 9L)))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
