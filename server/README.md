# 로컬 실행 (Docker Compose)

`docker compose up -d --build` 한 번으로 DB, Kafka, 앱이 모두 뜬다. (이슈 #91)

## 빠르게 시작하기

```bash
docker compose up -d --build
curl -H "X-API-KEY: dev-local-key" http://127.0.0.1:8080/api/study/topics
```

앱은 `127.0.0.1:8080`, Postgres는 `127.0.0.1:5432`, Kafka는 `127.0.0.1:9092`에서만 열린다 —
같은 네트워크의 다른 기기에서는 접근할 수 없다.

## 앱을 IDE/bootRun으로 개발할 때

컨테이너 앱 대신 IDE에서 디버깅하며 개발하려면 DB/Kafka만 띄우고 앱은 `bootRun`으로 따로 실행한다.

```bash
docker compose up -d db kafka
./gradlew :api:bootRun
```

이때 앱은 `application.properties`의 기본값(`localhost:5432`, `localhost:9092`)을 그대로 쓴다.

## 구성 요약

- **db**: `postgres:16`. `pg_isready` 헬스체크 통과 후에야 앱이 기동한다.
- **kafka**: `apache/kafka:3.9.0`. 리스너가 둘이다 — `PLAINTEXT`(`localhost:9092`, 호스트에서 `bootRun`으로
  붙을 때)와 `INTERNAL`(`kafka:19092`, 같은 compose 네트워크의 `app` 컨테이너가 붙을 때). 컨테이너 안에서
  `localhost`는 자기 자신이라 리스너 하나로는 둘 다 처리할 수 없다.
- **app**: 이 저장소의 `Dockerfile`로 빌드(`prod` 프로파일). `db`/`kafka`가 healthy가 된 뒤에만 기동한다.
  이미지는 시크릿에 기본값이 없어서(`API_KEY`, `DB_PASSWORD` 미설정 시 기동 실패) compose가 로컬 전용 기본값
  (`dev-local-key` / `hubdash`)을 제공한다 — 바꾸려면 `server/.env`에 `API_KEY=...`, `DB_PASSWORD=...`를 적는다.
- Postgres 데이터(`hubdash-db-data`)와 Kafka 로그/오프셋(`hubdash-kafka-data`)은 named volume에 남아
  `docker compose down`(볼륨 삭제 없이) 후 다시 `up`해도 유지된다. 완전히 초기화하려면
  `docker compose down -v`.

## 노션 → 허브 링크 자동 동기화 (이슈 #107)

노션에 새로 만든 페이지를 허브 링크로 자동으로 가져온다. **노션 → HubDash 한 방향**이고 가져오는 건 페이지 제목과 주소뿐이다(본문은 노션에 그대로). 노션에서 제목을 바꾸거나 페이지를 지운 건 반영하지 않는다.

### 처음 한 번 설정
1. <https://www.notion.so/my-integrations>에서 **내부 통합(Internal integration)**을 만들고 토큰(`ntn_...`)을 복사한다. 권한은 "콘텐츠 읽기"만 있으면 된다.
2. 동기화할 노션 DB/페이지마다 오른쪽 위 `•••` → **연결(Connections)**에서 만든 통합을 추가한다. 연결하지 않은 DB는 API에서 404로 보여 그 소스만 실패로 기록된다.
3. `server/.env`에 `NOTION_TOKEN=ntn_...`을 넣고 `docker compose up -d`로 앱을 다시 띄운다.

토큰이 없으면 동기화만 꺼진 채로 앱은 정상 기동한다(로그: `NOTION_TOKEN이 없어 노션 동기화를 건너뜁니다`).

### 동작
- 동기화 소스 = 노션 DB(행이 대상) 또는 부모 페이지(바로 아래 하위 페이지가 대상) + 넣을 허브 카테고리
- 기동 30초 뒤 한 번, 이후 10분마다 돈다(`app.notion.sync-initial-delay`, `app.notion.sync-interval`). 바로 돌리려면 `POST /api/notion/sync`
- 새 페이지는 raw 허브 링크로 넣어 평소처럼 Kafka raw → ETL을 탄다
- 이미 가져온 페이지는 다시 넣지 않고, 카테고리에 같은 노션 페이지 링크가 이미 있으면(#94 이관분) 새로 만들지 않는다

### API
```bash
# 소스 등록 — notion에는 노션 ID나 주소를 그대로 넣어도 된다
curl -X POST -H "X-API-KEY: $API_KEY" -H "Content-Type: application/json" localhost:8080/api/notion/sources \
  -d '{"notion":"https://app.notion.com/p/<DB ID>","sourceType":"DATABASE","hubCategoryId":1}'
curl -H "X-API-KEY: $API_KEY" localhost:8080/api/notion/sources          # 목록
curl -X DELETE -H "X-API-KEY: $API_KEY" localhost:8080/api/notion/sources/1  # 삭제(만들어진 링크는 남음)
curl -X POST -H "X-API-KEY: $API_KEY" localhost:8080/api/notion/sync     # 지금 동기화, 소스별 결과 반환
```

과목(`/api/pknu/courses`)과 공부 주제(`/api/study/topics`)에는 해당 노션 필기 페이지 주소를 `notionUrl`로 저장할 수 있다.

## 검증한 것 (2026-09-22)

`down -v`로 볼륨까지 지운 뒤 처음부터 새로 띄워 확인했다.

- `docker compose up -d --build`로 DB → Kafka → 앱 순서로 기동, Flyway 마이그레이션(24개) 성공,
  Tomcat 정상 기동
- API 골든 패스(`POST /api/study/topics` 201, `GET` 200)와 실패 경로(API 키 없음/오답 401,
  필수값 누락 400) 응답 확인
- Kafka를 거치는 ETL 경로(`POST /api/schedule/raw/events` → Kafka 발행 → 컨슈머가 소비해 정규화된
  `Event` 생성)를 골든 패스(`PROCESSED`)와 실패 경로(날짜 파싱 실패 → `FAILED`, 재시도 없이 정상 종료)
  양쪽 다 확인
- `docker compose down`(볼륨 유지) 후 컨테이너를 완전히 새로 만들어 재기동 — Postgres 데이터
  (study_topic, raw_event, 정규화된 event)와 Kafka 컨슈머 오프셋(`kafka-consumer-groups.sh`로
  `CURRENT-OFFSET == LOG-END-OFFSET`, `LAG 0` 확인, 재처리로 인한 중복 없음) 모두 유지되는 것을 확인
