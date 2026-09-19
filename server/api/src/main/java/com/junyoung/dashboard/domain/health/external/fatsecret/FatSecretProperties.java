package com.junyoung.dashboard.domain.health.external.fatsecret;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// clientId/clientSecret은 환경변수(FATSECRET_CLIENT_ID/SECRET, 이슈 #52)로 주입한다. 비어 있어도 앱은 기동하고,
// FatSecret을 실제로 호출하는 시점에만 FatSecretException(NOT_CONFIGURED)이 난다.
@ConfigurationProperties("fatsecret")
public record FatSecretProperties(
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("https://oauth.fatsecret.com/connect/token") String tokenUrl,
        @DefaultValue("https://platform.fatsecret.com/rest/server.api") String apiUrl
) {
    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }
}
