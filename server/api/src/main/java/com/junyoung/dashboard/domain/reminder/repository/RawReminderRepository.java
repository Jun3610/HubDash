package com.junyoung.dashboard.domain.reminder.repository;

import com.junyoung.dashboard.domain.reminder.entity.RawReminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawReminderRepository extends JpaRepository<RawReminder, Long> {
}
