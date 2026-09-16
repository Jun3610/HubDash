package com.junyoung.dashboard.domain.reminder.service;

import com.junyoung.dashboard.domain.reminder.dto.ReminderRequest;
import com.junyoung.dashboard.domain.reminder.dto.ReminderResponse;
import com.junyoung.dashboard.domain.reminder.entity.Reminder;
import com.junyoung.dashboard.domain.reminder.repository.ReminderRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReminderService {

    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Transactional
    public ReminderResponse create(ReminderRequest request) {
        Reminder saved = reminderRepository.save(new Reminder(
                request.title(), request.targetAt(),
                request.targetDomain(), request.targetEntityId(), request.sent()));
        return ReminderResponse.from(saved);
    }

    public Page<ReminderResponse> findAll(Pageable pageable) {
        return reminderRepository.findAll(pageable)
                .map(ReminderResponse::from);
    }

    public ReminderResponse findById(Long id) {
        return ReminderResponse.from(getOrThrow(id));
    }

    @Transactional
    public ReminderResponse update(Long id, ReminderRequest request) {
        Reminder reminder = getOrThrow(id);
        reminder.update(request.title(), request.targetAt(),
                request.targetDomain(), request.targetEntityId(), request.sent());
        return ReminderResponse.from(reminder);
    }

    @Transactional
    public void delete(Long id) {
        reminderRepository.delete(getOrThrow(id));
    }

    private Reminder getOrThrow(Long id) {
        return reminderRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(Reminder.class, id));
    }
}
