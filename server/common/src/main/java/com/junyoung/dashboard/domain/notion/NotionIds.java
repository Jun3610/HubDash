package com.junyoung.dashboard.domain.notion;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 노션 ID는 표기가 여러 가지다 — UUID(하이픈 포함), 하이픈 없는 32자리, 주소 끝("제목-<32자리>", "/p/<32자리>?pvs=...").
// 비교/저장은 하이픈 없는 32자리 소문자 하나로 통일한다.
public final class NotionIds {

    private static final Pattern UUID_WITH_DASHES =
            Pattern.compile("([0-9a-fA-F]{8})-([0-9a-fA-F]{4})-([0-9a-fA-F]{4})-([0-9a-fA-F]{4})-([0-9a-fA-F]{12})");
    private static final Pattern COMPACT = Pattern.compile("(?<![0-9a-fA-F])([0-9a-fA-F]{32})(?![0-9a-fA-F])");

    private NotionIds() {
    }

    // 문자열 안의 마지막 노션 ID를 찾는다. 주소는 경로 끝에 페이지 ID가 오므로 마지막 것이 대상 페이지다.
    public static Optional<String> extract(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String withoutQuery = text.split("[?#]", 2)[0];
        String found = null;
        Matcher dashed = UUID_WITH_DASHES.matcher(withoutQuery);
        while (dashed.find()) {
            found = dashed.group(1) + dashed.group(2) + dashed.group(3) + dashed.group(4) + dashed.group(5);
        }
        if (found == null) {
            Matcher compact = COMPACT.matcher(withoutQuery);
            while (compact.find()) {
                found = compact.group(1);
            }
        }
        return Optional.ofNullable(found).map(id -> id.toLowerCase(Locale.ROOT));
    }

    // 링크로 저장할 주소 — #94 이관분과 같은 형태로 맞춘다.
    public static String pageUrl(String notionId) {
        return "https://app.notion.com/p/" + notionId;
    }
}
