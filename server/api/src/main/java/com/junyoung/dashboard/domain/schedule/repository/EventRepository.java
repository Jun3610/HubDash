package com.junyoung.dashboard.domain.schedule.repository;

import com.junyoung.dashboard.domain.schedule.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
