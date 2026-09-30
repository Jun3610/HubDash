package com.junyoung.dashboard.domain.integration.dto;

import com.junyoung.dashboard.domain.integration.entity.IntegrationProvider;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 연동 한 건의 상태 (이슈 #227). 비밀값은 절대 담지 않고 maskedSecret("ntn_…6SK")만.
 * scheduleDatabaseId는 노션만, calendars는 iCloud만 쓴다.
 */
public record IntegrationResponse(
        IntegrationProvider provider,
        boolean configured,
        String account,
        String maskedSecret,
        String scheduleDatabaseId,
        List<String> calendars,
        LocalDateTime lastVerifiedAt,
        String lastError
) {
}
