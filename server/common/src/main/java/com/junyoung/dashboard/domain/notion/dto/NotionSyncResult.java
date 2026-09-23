package com.junyoung.dashboard.domain.notion.dto;

// 소스 하나를 한 번 동기화한 결과.
// created: 새 링크로 넣은 페이지(raw → ETL로 보냄), matched: 카테고리에 이미 있던 링크와 짝지은 페이지,
// skipped: 제목이 비어 있어 넣지 않은 페이지. error가 있으면 이 소스는 실패했고 나머지 소스는 계속 돈다.
public record NotionSyncResult(
        Long sourceId,
        String notionId,
        int created,
        int matched,
        int skipped,
        String error
) {
    public static NotionSyncResult success(Long sourceId, String notionId, int created, int matched, int skipped) {
        return new NotionSyncResult(sourceId, notionId, created, matched, skipped, null);
    }

    public static NotionSyncResult failure(Long sourceId, String notionId, String error) {
        return new NotionSyncResult(sourceId, notionId, 0, 0, 0, error);
    }
}
