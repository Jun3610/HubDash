package com.junyoung.dashboard.domain.integration.repository;

import com.junyoung.dashboard.domain.integration.entity.IntegrationCredential;
import com.junyoung.dashboard.domain.integration.entity.IntegrationProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegrationCredentialRepository extends JpaRepository<IntegrationCredential, Long> {
    Optional<IntegrationCredential> findByProvider(IntegrationProvider provider);
}
