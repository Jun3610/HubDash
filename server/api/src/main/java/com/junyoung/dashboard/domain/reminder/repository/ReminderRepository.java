package com.junyoung.dashboard.domain.reminder.repository;

import com.junyoung.dashboard.domain.reminder.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
}
