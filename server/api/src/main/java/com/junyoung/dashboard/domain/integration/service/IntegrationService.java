package com.junyoung.dashboard.domain.integration.service;

import com.junyoung.dashboard.domain.integration.dto.CalendarOption;
import com.junyoung.dashboard.domain.integration.dto.ICloudIntegrationRequest;
import com.junyoung.dashboard.domain.integration.dto.IntegrationResponse;
import com.junyoung.dashboard.domain.integration.dto.IntegrationSaveResponse;
import com.junyoung.dashboard.domain.integration.dto.IntegrationTestResponse;
import com.junyoung.dashboard.domain.integration.dto.NotionIntegrationRequest;
import com.junyoung.dashboard.domain.integration.entity.IntegrationCredential;
import com.junyoung.dashboard.domain.integration.entity.IntegrationProvider;
import com.junyoung.dashboard.domain.integration.repository.IntegrationCredentialRepository;
import com.junyoung.dashboard.global.crypto.CredentialCipher;
import com.junyoung.dashboard.global.exception.IntegrationException;
import com.junyoung.dashboard.global.icloud.CalDavCalendar;
import com.junyoung.dashboard.global.icloud.CalDavClient;
import com.junyoung.dashboard.global.icloud.ICloudException;
import com.junyoung.dashboard.global.icloud.ICloudLogin;
import com.junyoung.dashboard.global.notion.NotionAccount;
import com.junyoung.dashboard.global.notion.NotionClient;
import com.junyoung.dashboard.global.notion.NotionException;
import com.junyoung.dashboard.global.notion.NotionIds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 웹 설정 화면의 연동 계정 관리 (이슈 #227).
 * 저장하기 전에 실제로 로그인해 보고(노션 /users/me, iCloud CalDAV), 실패하면 저장하지 않는다.
 * 응답에는 비밀값을 넣지 않는다 — 마스킹한 값만.
 */
@Service
public class IntegrationService {

    private final IntegrationCredentialRepository repository;
    private final CredentialStore store;
    private final CredentialCipher cipher;
    private final NotionClient notion;
    private final CalDavClient caldav;
    private final Set<String> duplicateCalendars;
    private final Clock clock;

    @Autowired
    public IntegrationService(IntegrationCredentialRepository repository, CredentialStore store, CredentialCipher cipher,
                              NotionClient notion, CalDavClient caldav,
                              @Value("${app.icloud.duplicate-calendars:}") String duplicateCalendars) {
        this(repository, store, cipher, notion, caldav, duplicateCalendars, Clock.systemDefaultZone());
    }

    IntegrationService(IntegrationCredentialRepository repository, CredentialStore store, CredentialCipher cipher,
                       NotionClient notion, CalDavClient caldav, String duplicateCalendars, Clock clock) {
        this.repository = repository;
        this.store = store;
        this.cipher = cipher;
        this.notion = notion;
        this.caldav = caldav;
        this.duplicateCalendars = Arrays.stream(duplicateCalendars.split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<IntegrationResponse> list() {
        return List.of(notionView(), icloudView());
    }

    @Transactional
    public IntegrationSaveResponse putNotion(NotionIntegrationRequest request) {
        cipher.requireReady();
        String token = blankToNull(request.token());
        if (token == null) {
            token = store.currentToken(); // 비우면 지금 쓰는 토큰(저장값 → 환경변수)을 그대로
        }
        if (token == null) {
            throw invalid("노션 토큰을 입력해 주세요");
        }
        NotionSettings before = store.notionSettings();
        String databaseId = before.scheduleDatabaseId();
        if (request.scheduleDatabase() != null) {
            databaseId = request.scheduleDatabase().isBlank() ? null : NotionIds.extract(request.scheduleDatabase());
            if (!request.scheduleDatabase().isBlank() && databaseId == null) {
                throw invalid("노션 일정 DB 주소나 ID를 알아볼 수 없어요");
            }
        }
        NotionAccount account;
        try {
            account = notion.me(token);
        } catch (NotionException e) {
            throw invalid("노션 연결 확인 실패 — " + e.getMessage());
        }
        String secret = cipher.encrypt(token);
        IntegrationCredential c = credential(IntegrationProvider.NOTION);
        c.save(CredentialStore.write(new NotionSettings(account.botId(), account.workspaceName(), databaseId)), secret, now());
        repository.save(c);
        boolean changed = before.botId() != null && !before.botId().equals(account.botId());
        return new IntegrationSaveResponse(notionView(), changed);
    }

    @Transactional
    public IntegrationSaveResponse putICloud(ICloudIntegrationRequest request) {
        cipher.requireReady();
        ICloudSettings before = store.icloudSettings();
        String appleId = Objects.requireNonNullElse(blankToNull(request.appleId()), Objects.toString(before.appleId(), ""));
        if (appleId.isBlank()) {
            throw invalid("Apple ID를 입력해 주세요");
        }
        String password = blankToNull(request.appPassword());
        if (password == null) {
            password = store.secret(IntegrationProvider.ICLOUD);
        }
        if (password == null) {
            throw invalid("앱 전용 암호를 입력해 주세요");
        }
        List<String> calendars = request.calendars() == null ? before.calendars()
                : request.calendars().stream().map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
        try {
            caldav.principal(new ICloudLogin(appleId.strip(), password));
        } catch (ICloudException e) {
            throw invalid("iCloud 연결 확인 실패 — " + e.getMessage());
        }
        String secret = cipher.encrypt(password);
        IntegrationCredential c = credential(IntegrationProvider.ICLOUD);
        c.save(CredentialStore.write(new ICloudSettings(appleId.strip(), calendars)), secret, now());
        repository.save(c);
        boolean changed = before.appleId() != null && !before.appleId().equalsIgnoreCase(appleId.strip());
        return new IntegrationSaveResponse(icloudView(), changed);
    }

    /** 로그인된 iCloud 계정의 캘린더 — 저장된 선택과 중복 경고를 함께 */
    @Transactional(readOnly = true)
    public List<CalendarOption> icloudCalendars() {
        ICloudLogin login = store.icloudLogin();
        if (login == null) {
            throw invalid("iCloud 계정을 먼저 저장해 주세요");
        }
        List<String> selected = store.icloudSettings().calendars();
        try {
            return caldav.calendars(login).stream()
                    .map(CalDavCalendar::name)
                    .distinct()
                    .map(name -> new CalendarOption(name, selected.contains(name), duplicateWarning(name)))
                    .toList();
        } catch (ICloudException e) {
            throw new IntegrationException(HttpStatus.BAD_GATEWAY, "ICLOUD_ERROR", e.getMessage());
        }
    }

    /** 연결 확인만 — 결과를 last_verified_at / last_error에 남긴다 */
    @Transactional
    public IntegrationTestResponse test(IntegrationProvider provider) {
        String account = null;
        String error = null;
        try {
            account = provider == IntegrationProvider.NOTION ? testNotion() : testICloud();
        } catch (NotionException | ICloudException | IntegrationException e) {
            error = e.getMessage();
        }
        LocalDateTime at = now();
        String finalError = error;
        store.find(provider).ifPresent(c -> {
            if (finalError == null) {
                c.markVerified(at);
            } else {
                c.markFailed(finalError);
            }
        });
        return new IntegrationTestResponse(provider, error == null, account, error, at);
    }

    /** 저장값만 지운다 — 이미 가져온 일정은 그대로 */
    @Transactional
    public void delete(IntegrationProvider provider) {
        store.find(provider).ifPresent(repository::delete);
    }

    private String testNotion() {
        String token = store.currentToken();
        if (token == null) {
            throw new NotionException("노션 토큰이 없어요");
        }
        NotionAccount account = notion.me(token);
        String db = store.notionSettings().scheduleDatabaseId();
        if (db != null) {
            notion.queryDatabaseRows(db); // 통합이 일정 DB에 연결 안 됐으면 여기서 안내 문구와 함께 실패
        }
        return account.workspaceName();
    }

    private String testICloud() {
        ICloudLogin login = store.icloudLogin();
        if (login == null) {
            throw new ICloudException("iCloud 계정이 저장돼 있지 않아요");
        }
        caldav.principal(login);
        return login.appleId();
    }

    private IntegrationResponse notionView() {
        IntegrationCredential c = store.find(IntegrationProvider.NOTION).orElse(null);
        NotionSettings s = store.notionSettings();
        String token = store.currentToken();
        return new IntegrationResponse(IntegrationProvider.NOTION, token != null,
                s.workspaceName(), token == null ? null : mask(token), s.scheduleDatabaseId(), null,
                c == null ? null : c.getLastVerifiedAt(), c == null ? null : c.getLastError());
    }

    private IntegrationResponse icloudView() {
        IntegrationCredential c = store.find(IntegrationProvider.ICLOUD).orElse(null);
        ICloudSettings s = store.icloudSettings();
        boolean configured = c != null && c.getSecretEnc() != null && s.appleId() != null;
        return new IntegrationResponse(IntegrationProvider.ICLOUD, configured,
                s.appleId(), configured ? "…" + tail(maskSourceICloud(c)) : null, null, s.calendars(),
                c == null ? null : c.getLastVerifiedAt(), c == null ? null : c.getLastError());
    }

    // 앱 암호를 못 풀면(키가 바뀜) 마스킹도 못 한다 — 빈 꼬리
    private String maskSourceICloud(IntegrationCredential c) {
        try {
            return cipher.decrypt(c.getSecretEnc());
        } catch (IntegrationException e) {
            return "";
        }
    }

    /**
     * "ntn_…6SK" — 노션 토큰은 앞의 형식 표시(ntn_ / secret_)와 끝 3자만 보여 준다.
     * 형식 표시가 없거나 너무 짧으면 끝 3자만.
     */
    static String mask(String secret) {
        int underscore = secret.indexOf('_');
        String prefix = underscore > 0 && underscore <= 6 && secret.length() > underscore + 8 ? secret.substring(0, underscore + 1) : "";
        return prefix + "…" + tail(secret);
    }

    private static String tail(String secret) {
        return secret.length() >= 10 ? secret.substring(secret.length() - 3) : "";
    }

    private String duplicateWarning(String calendarName) {
        return duplicateCalendars.contains(calendarName)
                ? "노션 일정을 옮겨 둔 캘린더예요 — 가져오면 노션에서 가져온 일정과 중복돼요"
                : null;
    }

    private IntegrationCredential credential(IntegrationProvider provider) {
        return store.find(provider).orElseGet(() -> new IntegrationCredential(provider));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private static IntegrationException invalid(String message) {
        return new IntegrationException(HttpStatus.BAD_REQUEST, "INTEGRATION_INVALID", message);
    }
}
