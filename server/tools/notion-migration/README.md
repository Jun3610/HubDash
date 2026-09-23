# 노션 데이터 일회성 이관 (이슈 #94)

노션 대시보드에 쓰던 데이터를 로컬 HubDash로 **한 번 복사**해 넣는 도구다. 노션과 동기화하지 않는다.
2026-09-23에 한 번 실행했고, 같은 데이터를 다시 넣을 일은 없다.

## 구성
- `migrate.py` — 변환된 JSON을 읽어 서버 API로 넣고, ETL 처리 결과와 건수를 검증한다.
- `data/notion-export.json` — 노션에서 읽어 변환한 입력. 개인 일정이 들어 있어 **커밋하지 않는다**(`.gitignore`).

## 흐름
1. Claude의 노션 커넥터로 노션 DB/페이지를 읽어 `data/notion-export.json`으로 변환
2. `python3 migrate.py data/notion-export.json`
   - 대상 테이블에 데이터가 하나라도 있으면 중복 이관을 막으려고 멈춘다
   - 일정/메모/허브 링크는 raw 엔드포인트로 넣어 평소 입력과 같은 Kafka raw → ETL 경로를 탄다
   - ID가 바로 필요한 부모(허브 카테고리, 공부 주제, 학기, 과목)와 프로필은 일반 API로 넣는다
   - 마지막에 raw 처리 결과(PROCESSED/FAILED)와 도메인별 건수를 기대값과 대조한다

## 매핑

| 노션 | HubDash | 비고 |
|---|---|---|
| Schedule DB (321건) | 일정 | 시간은 UTC로 읽혀서 KST로 변환. 날짜만 있으면 하루 종일(끝은 다음 날 0시), 끝 시간이 없거나 시작과 같으면 1시간으로 둠. Status(Completed/Scheduled)는 대응 필드가 없어 버림 |
| HUB의 Docker/Network/Java DB, CI/CD·CS·CT·License 페이지 | 허브 카테고리 4개 + 링크 21개 | 링크 URL은 노션 페이지 주소, 설명에 상태/마감일. 행이 비어 있는 Infra Structure, 행이 없는 Scripts는 제외 |
| Study 2026 (Java, Data Structure, Algorithm, DB, SQLD) | 공부 주제 5개 | 학습 기록(StudyProgress)은 노션에 학습 시간이 없어 만들지 않음 |
| PKNU 2026-1, 2026-2 과목 페이지 | 학기 2개 + 과목 11개 | 학기 기간은 노션 일정의 개강/방학일에서, 학점은 과목 페이지 머리말과 학기 합계에서. 2026-1은 학점 표기가 없어 3학점으로 둠 |
| LIFE, Health 콜아웃, Memo 토글 4개 | 메모 6개 | 태그 `notion,...` |
| 프로필 콜아웃 | 사용자 프로필 | 이름과 이력 한 줄 요약 |
| Dummy(템플릿/미사용 DB) | 제외 | |
