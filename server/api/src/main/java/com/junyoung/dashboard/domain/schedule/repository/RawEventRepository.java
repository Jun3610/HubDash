package com.junyoung.dashboard.domain.schedule.repository;

import com.junyoung.dashboard.domain.schedule.entity.RawEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawEventRepository extends JpaRepository<RawEvent, Long> {
}
