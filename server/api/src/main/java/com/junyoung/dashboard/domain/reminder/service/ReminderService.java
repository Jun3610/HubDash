package com.junyoung.dashboard.domain.reminder.service;

import com.junyoung.dashboard.domain.reminder.dto.ReminderRequest;
import com.junyoung.dashboard.domain.reminder.dto.ReminderResponse;
import com.junyoung.dashboard.domain.reminder.entity.Reminder;
import com.junyoung.dashboard.domain.reminder.repository.ReminderRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public List<ReminderResponse> findAll() {
        return reminderRepository.findAll().stream()
                .map(ReminderResponse::from)
                .toList();
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
                .orElseThrow(() -> new EntityNotFoundException("reminder " + id + " not found"));
    }
}
