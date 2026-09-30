package com.junyoung.dashboard.domain.sync.repository;

import com.junyoung.dashboard.domain.sync.entity.SyncRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {

    Optional<SyncRun> findFirstBySourceOrderByStartedAtDescIdDesc(String source);

    boolean existsBySourceAndOkTrue(String source);
}
