package com.junyoung.dashboard.global.notion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 노션 주소·ID에서 대시 없는 32자리 ID를 뽑는다 (이슈 #163) */
public final class NotionIds {

    // 대시 있는 UUID(8-4-4-4-12)나 대시 없는 32자리. 주소 끝쪽에 있는 것을 쓴다
    private static final Pattern ID = Pattern.compile(
            "([0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12})");

    private NotionIds() {
    }

    /** 없거나 못 찾으면 null */
    public static String extract(String urlOrId) {
        if (urlOrId == null || urlOrId.isBlank()) {
            return null;
        }
        String path = urlOrId.split("[?#]")[0];
        Matcher m = ID.matcher(path);
        String last = null;
        while (m.find()) {
            last = m.group(1);
        }
        return last == null ? null : last.replace("-", "").toLowerCase();
    }

    /** HubDash 링크로 저장할 노션 페이지 주소 (이관 때와 같은 모양) */
    public static String pageUrl(String id) {
        return "https://app.notion.com/p/" + id.replace("-", "").toLowerCase();
    }
}
