package com.junyoung.dashboard.domain.notion.client;

import java.util.List;

public interface NotionClient {

    // 토큰이 없으면 동기화 기능 전체가 꺼진 상태로 동작한다.
    boolean isConfigured();

    // DB의 행(페이지) 전체. 휴지통에 있는 페이지는 제외한다.
    List<NotionPage> listDatabasePages(String databaseId);

    // 부모 페이지 바로 아래의 하위 페이지. 하위 DB나 일반 블록은 제외한다.
    List<NotionPage> listChildPages(String pageId);
}
