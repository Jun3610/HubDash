# study 도메인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `study` 도메인(StudyTopic 1:N StudyProgress)의 CRUD API를 hub 도메인과 동일한 패턴으로 구현한다. hub 도메인 구현 중 발견된 함정(아래 Known Pitfalls 참고)을 이번 계획에서는 미리 반영해 재발을 막는다.

**Architecture:** `common` 모듈에 Entity+DTO, `api` 모듈에 Repository/Service/Controller를 둔다. Flyway로 `study_topic`, `study_progress` 테이블을 만들고(V2 마이그레이션 — V1은 hub 도메인이 이미 사용 중), API Key 인증 + `ApiResponse<T>` 래퍼 + `GlobalExceptionHandler`를 그대로 재사용한다.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway(Postgres), H2(테스트), Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md`

**Branch:** `feature/domain-study-4` (이슈 #4 하위 작업이므로 브랜치명에 이슈 번호 포함 — hub 도메인 때와 동일 컨벤션)

## Known Pitfalls (hub 도메인 구현에서 발견, 이번 계획은 이미 반영함)

1. **Jackson은 3버전이다.** 이 프로젝트의 Spring Boot 4.1.1은 Jackson 3을 쓴다. 테스트에서 ObjectMapper를 쓸 때는 반드시 `tools.jackson.databind.ObjectMapper`를 import한다 (`com.fasterxml.jackson.databind.ObjectMapper`가 아님). 이 계획의 모든 코드 스니펫은 이미 올바른 import를 쓴다.
2. **테스트 어노테이션 패키지가 표준과 다르다.** `@WebMvcTest`/`@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest`는 `org.springframework.boot.data.jpa.test.autoconfigure`, `TestEntityManager`는 `org.springframework.boot.jpa.test.autoconfigure`에 있다. 이미 `api/build.gradle`에 필요한 스타터(`spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-validation-test`)가 있으니 **build.gradle을 건드릴 필요가 없다** — 컴파일 에러가 나면 의존성이 없는 게 아니라 import 경로가 틀린 것이다. 절대 build.gradle에 의존성을 추가/삭제하지 말고, import를 의심하라.
3. **Flyway 스타터는 이미 있다.** `spring-boot-starter-flyway`가 hub 도메인 작업 중 추가되어 이미 `api/build.gradle`에 있다. 건드릴 필요 없음.
4. **테스트가 "이름은 검증을 약속하지만 실제로 검증 안 하는" 함정을 피해야 한다.** 예: `save()`를 스텁할 때 요청과 같은 리터럴로 새 엔티티를 만들어 리턴하면, 서비스가 요청을 완전히 무시해도 테스트가 통과한다. **Service의 create/update 테스트는 항상 `ArgumentCaptor`로 실제 `save()`/mutate에 전달된 값을 캡처해서 검증한다** (unpersisted 엔티티의 id 비교처럼 null==null이 되는 비교는 쓰지 않는다).
5. **update()는 요청의 모든 필드를 실제로 반영해야 한다.** hub 도메인에서 `HubLinkService.update()`가 `categoryId`를 받고도 무시하는 버그가 있었다(필수 필드인데도 조용히 무시 → 200 응답). `StudyProgressService.update()`는 `topicId`를 포함한 요청의 모든 필드를 실제로 반영해야 하며, 이번 계획의 코드는 이미 그렇게 작성되어 있다.
6. **FK 컬럼에는 처음부터 인덱스를 만든다.** `study_progress.topic_id`에 인덱스를 V2 마이그레이션에 바로 포함한다 (나중에 V3로 추가하지 않는다).
7. **`server/` 디렉토리에서 `./gradlew` 실행.** 레포 루트가 아니라 `server/` 안에서 모든 gradle 명령을 실행한다.

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- Entity는 `common`, Repository/Service/Controller는 `api`.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt) 상속.
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- API 경로 컨벤션: `/api/study/...`.
- 스키마 관리는 Flyway. 운영은 `ddl-auto=validate`, 테스트는 `spring.flyway.enabled=false` + `ddl-auto=create-drop` + H2 (`spring.test.database.replace=none`로 MODE=PostgreSQL H2 URL이 실제로 적용되도록 — `application-test.properties`는 hub 도메인 때 이미 이렇게 구성돼있으니 공유해서 쓴다, 새로 안 만든다).
- 모든 @DataJpaTest는 @Import(JpaAuditingConfig.class)를 함께 선언해야 createdAt/updatedAt이 채워진다.
- 이 계획을 시작하기 전에 `main`에 hub 도메인(PR #12)이 이미 머지되어 있어야 한다 (BaseEntity, ApiResponse, GlobalExceptionHandler, ApiKeyAuthFilter, JpaAuditingConfig, `application-test.properties`, `spring-boot-starter-flyway`가 이미 존재한다고 가정).

## 엔티티 설계 (컨트롤러가 스펙을 보고 확정)

- `StudyTopic`(부모): `name`(필수, 100자), `description`(선택, 500자) — HubCategory와 동일한 모양.
- `StudyProgress`(자식): `topic`(FK, 필수), `studiedAt`(LocalDate, 필수 — 공부한 날짜), `minutes`(Integer, 필수 — 공부한 시간(분)), `notes`(String, 선택, 1000자 — 메모).

---

### Task 1: StudyTopic 엔티티 + Flyway + Repository

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/study/entity/StudyTopic.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/study/entity/StudyProgress.java` (컴파일용 최소 버전만, Task 5에서 완성)
- Create: `server/api/src/main/resources/db/migration/V2__study.sql`
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/study/repository/StudyTopicRepository.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/study/repository/StudyTopicRepositoryTest.java`

**Interfaces:**
- Consumes: `BaseEntity`
- Produces: `class StudyTopic extends BaseEntity`(`getName()`, `getDescription()`, 생성자 `StudyTopic(String name, String description)`, `void update(String name, String description)`), `interface StudyTopicRepository extends JpaRepository<StudyTopic, Long>` — Task 3(Service)이 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class StudyTopicRepositoryTest {

    @Autowired
    private StudyTopicRepository studyTopicRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        StudyTopic saved = studyTopicRepository.save(new StudyTopic("토익", "영어 공부"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.repository.StudyTopicRepositoryTest"`
Expected: FAIL (컴파일 에러 — `StudyTopic`, `StudyTopicRepository`가 없음)

- [ ] **Step 3: StudyTopic 엔티티 구현**

```java
package com.junyoung.dashboard.domain.study.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study_topic")
public class StudyTopic extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyProgress> progresses = new ArrayList<>();

    public StudyTopic(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
```

- [ ] **Step 4: StudyProgress 최소 버전 작성 (컴파일용, Task 5에서 완성)**

```java
package com.junyoung.dashboard.domain.study.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study_progress")
public class StudyProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private StudyTopic topic;
}
```

- [ ] **Step 5: StudyTopicRepository 구현**

```java
package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyTopicRepository extends JpaRepository<StudyTopic, Long> {
}
```

- [ ] **Step 6: Flyway 마이그레이션 작성 (인덱스 포함)**

`server/api/src/main/resources/db/migration/V2__study.sql`:

```sql
CREATE TABLE study_topic (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE study_progress (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES study_topic(id),
    studied_at DATE NOT NULL,
    minutes INTEGER NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_study_progress_topic_id ON study_progress(topic_id);
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.repository.StudyTopicRepositoryTest"`
Expected: PASS

- [ ] **Step 8: 커밋**

```bash
git add common/src api/src
git commit -m "feat: StudyTopic 엔티티, Flyway 마이그레이션, Repository 추가"
```

---

### Task 2: StudyTopic DTO

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/study/dto/StudyTopicRequest.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/study/dto/StudyTopicResponse.java`
- Test: `server/common/src/test/java/com/junyoung/dashboard/domain/study/dto/StudyTopicResponseTest.java`

**Interfaces:**
- Consumes: `StudyTopic`(Task 1)
- Produces: `record StudyTopicRequest(String name, String description)`, `record StudyTopicResponse(Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt)`와 `StudyTopicResponse.from(StudyTopic)` — Task 3이 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudyTopicResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        StudyTopic topic = new StudyTopic("토익", "영어 공부");

        StudyTopicResponse response = StudyTopicResponse.from(topic);

        assertThat(response.name()).isEqualTo("토익");
        assertThat(response.description()).isEqualTo("영어 공부");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.study.dto.StudyTopicResponseTest"`
Expected: FAIL

- [ ] **Step 3: StudyTopicRequest 구현**

```java
package com.junyoung.dashboard.domain.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyTopicRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
) {
}
```

- [ ] **Step 4: StudyTopicResponse 구현**

```java
package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.StudyTopic;

import java.time.LocalDateTime;

public record StudyTopicResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudyTopicResponse from(StudyTopic topic) {
        return new StudyTopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getCreatedAt(),
                topic.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.study.dto.StudyTopicResponseTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add common/src
git commit -m "feat: StudyTopic 요청/응답 DTO 추가"
```

---

### Task 3: StudyTopicService

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/study/service/StudyTopicService.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/study/service/StudyTopicServiceTest.java`

**Interfaces:**
- Consumes: `StudyTopicRepository`(Task 1), `StudyTopicRequest`/`StudyTopicResponse`(Task 2), `EntityNotFoundException`
- Produces: `class StudyTopicService`의 `create(StudyTopicRequest)`, `findAll()`, `findById(Long)`, `update(Long, StudyTopicRequest)`, `delete(Long)` — Task 4가 사용.

**중요 (Known Pitfall #4):** `create()` 테스트는 `save()`에 실제로 전달된 인자를 `ArgumentCaptor`로 캡처해서 name/description을 검증한다. 단순히 stub의 리턴값과 비교하는 tautological 테스트를 만들지 않는다.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicResponse;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyTopicServiceTest {

    @Mock
    private StudyTopicRepository studyTopicRepository;

    private StudyTopicService studyTopicService;

    @BeforeEach
    void setUp() {
        studyTopicService = new StudyTopicService(studyTopicRepository);
    }

    @Test
    void createsTopicUsingRequestFields() {
        StudyTopicRequest request = new StudyTopicRequest("토익", "영어 공부");
        when(studyTopicRepository.save(any(StudyTopic.class)))
                .thenReturn(new StudyTopic("토익", "영어 공부"));

        StudyTopicResponse response = studyTopicService.create(request);

        assertThat(response.name()).isEqualTo("토익");

        ArgumentCaptor<StudyTopic> captor = ArgumentCaptor.forClass(StudyTopic.class);
        verify(studyTopicRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo(request.name());
        assertThat(captor.getValue().getDescription()).isEqualTo(request.description());
    }

    @Test
    void throwsWhenTopicNotFound() {
        when(studyTopicRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyTopicService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.service.StudyTopicServiceTest"`
Expected: FAIL

- [ ] **Step 3: StudyTopicService 구현**

```java
package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicResponse;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudyTopicService {

    private final StudyTopicRepository studyTopicRepository;

    public StudyTopicService(StudyTopicRepository studyTopicRepository) {
        this.studyTopicRepository = studyTopicRepository;
    }

    @Transactional
    public StudyTopicResponse create(StudyTopicRequest request) {
        StudyTopic saved = studyTopicRepository.save(
                new StudyTopic(request.name(), request.description()));
        return StudyTopicResponse.from(saved);
    }

    public List<StudyTopicResponse> findAll() {
        return studyTopicRepository.findAll().stream()
                .map(StudyTopicResponse::from)
                .toList();
    }

    public StudyTopicResponse findById(Long id) {
        return StudyTopicResponse.from(getOrThrow(id));
    }

    @Transactional
    public StudyTopicResponse update(Long id, StudyTopicRequest request) {
        StudyTopic topic = getOrThrow(id);
        topic.update(request.name(), request.description());
        return StudyTopicResponse.from(topic);
    }

    @Transactional
    public void delete(Long id) {
        studyTopicRepository.delete(getOrThrow(id));
    }

    private StudyTopic getOrThrow(Long id) {
        return studyTopicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("study topic " + id + " not found"));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.service.StudyTopicServiceTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: StudyTopicService 추가"
```

---

### Task 4: StudyTopicController

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/study/controller/StudyTopicController.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/study/controller/StudyTopicControllerTest.java`

**Interfaces:**
- Consumes: `StudyTopicService`(Task 3), `ApiResponse`/`GlobalExceptionHandler`
- Produces: `POST/GET/PUT/DELETE /api/study/topics` 엔드포인트.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicResponse;
import com.junyoung.dashboard.domain.study.service.StudyTopicService;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyTopicController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudyTopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyTopicService studyTopicService;

    @Test
    void createsTopic() throws Exception {
        StudyTopicRequest request = new StudyTopicRequest("토익", "영어 공부");
        StudyTopicResponse response = new StudyTopicResponse(1L, "토익", "영어 공부",
                LocalDateTime.now(), LocalDateTime.now());
        when(studyTopicService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/study/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("토익"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        StudyTopicRequest invalid = new StudyTopicRequest("", null);

        mockMvc.perform(post("/api/study/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsTopics() throws Exception {
        when(studyTopicService.findAll()).thenReturn(List.of(
                new StudyTopicResponse(1L, "토익", null, LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/study/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("토익"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.controller.StudyTopicControllerTest"`
Expected: FAIL

- [ ] **Step 3: StudyTopicController 구현**

```java
package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicResponse;
import com.junyoung.dashboard.domain.study.service.StudyTopicService;
import com.junyoung.dashboard.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/study/topics")
public class StudyTopicController {

    private final StudyTopicService studyTopicService;

    public StudyTopicController(StudyTopicService studyTopicService) {
        this.studyTopicService = studyTopicService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudyTopicResponse> create(@Valid @RequestBody StudyTopicRequest request) {
        return ApiResponse.success(studyTopicService.create(request));
    }

    @GetMapping
    public ApiResponse<List<StudyTopicResponse>> findAll() {
        return ApiResponse.success(studyTopicService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<StudyTopicResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(studyTopicService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudyTopicResponse> update(@PathVariable Long id, @Valid @RequestBody StudyTopicRequest request) {
        return ApiResponse.success(studyTopicService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studyTopicService.delete(id);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.controller.StudyTopicControllerTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: StudyTopicController 추가"
```

---

### Task 5: StudyProgress 완성 (엔티티/Repository)

**Files:**
- Modify: `server/common/src/main/java/com/junyoung/dashboard/domain/study/entity/StudyProgress.java` (Task 1의 최소 버전을 완성)
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/study/repository/StudyProgressRepository.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/study/repository/StudyProgressRepositoryTest.java`

**Interfaces:**
- Consumes: `StudyTopic`(Task 1)
- Produces: `class StudyProgress extends BaseEntity`(`getTopic()`, `getStudiedAt()`, `getMinutes()`, `getNotes()`, 생성자 `StudyProgress(StudyTopic, LocalDate, Integer, String)`, **`void update(StudyTopic topic, LocalDate studiedAt, Integer minutes, String notes)`** — Known Pitfall #5에 따라 topic까지 update 시그니처에 포함), `interface StudyProgressRepository extends JpaRepository<StudyProgress, Long>`의 `findByTopicId(Long)` — Task 7이 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class StudyProgressRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudyProgressRepository studyProgressRepository;

    @Test
    void findsProgressesByTopicId() {
        StudyTopic topic = entityManager.persistAndFlush(new StudyTopic("토익", null));
        entityManager.persistAndFlush(new StudyProgress(topic, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이"));

        List<StudyProgress> progresses = studyProgressRepository.findByTopicId(topic.getId());

        assertThat(progresses).hasSize(1);
        assertThat(progresses.get(0).getMinutes()).isEqualTo(60);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.repository.StudyProgressRepositoryTest"`
Expected: FAIL

- [ ] **Step 3: StudyProgress 엔티티 완성**

```java
package com.junyoung.dashboard.domain.study.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study_progress")
public class StudyProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private StudyTopic topic;

    @Column(name = "studied_at", nullable = false)
    private LocalDate studiedAt;

    @Column(nullable = false)
    private Integer minutes;

    @Column(length = 1000)
    private String notes;

    public StudyProgress(StudyTopic topic, LocalDate studiedAt, Integer minutes, String notes) {
        this.topic = topic;
        this.studiedAt = studiedAt;
        this.minutes = minutes;
        this.notes = notes;
    }

    public void update(StudyTopic topic, LocalDate studiedAt, Integer minutes, String notes) {
        this.topic = topic;
        this.studiedAt = studiedAt;
        this.minutes = minutes;
        this.notes = notes;
    }
}
```

- [ ] **Step 4: StudyProgressRepository 구현**

```java
package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyProgressRepository extends JpaRepository<StudyProgress, Long> {
    List<StudyProgress> findByTopicId(Long topicId);
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.repository.StudyProgressRepositoryTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add common/src api/src
git commit -m "feat: StudyProgress 엔티티 완성과 Repository 추가"
```

---

### Task 6: StudyProgress DTO

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/study/dto/StudyProgressRequest.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/study/dto/StudyProgressResponse.java`
- Test: `server/common/src/test/java/com/junyoung/dashboard/domain/study/dto/StudyProgressResponseTest.java`

**Interfaces:**
- Consumes: `StudyProgress`, `StudyTopic`(Task 5)
- Produces: `record StudyProgressRequest(Long topicId, LocalDate studiedAt, Integer minutes, String notes)`, `record StudyProgressResponse(...)`와 `StudyProgressResponse.from(StudyProgress)` — Task 7이 사용.

**중요 (Known Pitfall #4):** 테스트는 topicId 매핑도 실제로 검증한다 (title/minutes만 확인하고 topicId를 빠뜨리지 않는다). topic이 영속화되지 않아 id가 null인 경우, `category.getId()`끼리 비교하는 게 아니라 `topic.getId()`가 null이 아님을 별도로 보장하도록 `entityManager.persistAndFlush`를 쓰거나, 혹은 Task 6에서는 DTO 단위 테스트이므로 `TestEntityManager` 없이도 `ReflectionTestUtils` 등으로 id를 세팅하지 말고 — 아래 Step 1처럼 **`topic.getId()`가 null이어도 `response.topicId()`가 그 null과 정확히 같음을 확인하는 대신, topic을 먼저 만들고 별도 필드 비교가 아닌 실제 매핑 로직이 올바른 필드(topic의 참조 자체)에서 오는지 확인**하도록 작성한다. 구체적으로는 아래 테스트 코드를 그대로 따른다 — 이미 이 문제를 피하도록 작성돼 있다.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StudyProgressResponseTest {

    @Test
    void mapsEntityFieldsIncludingTopicId() {
        StudyTopic topic = new StudyTopic("토익", null);
        ReflectionTestUtils.setField(topic, "id", 1L);
        StudyProgress progress = new StudyProgress(topic, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이");

        StudyProgressResponse response = StudyProgressResponse.from(progress);

        assertThat(response.minutes()).isEqualTo(60);
        assertThat(response.notes()).isEqualTo("RC 문제풀이");
        assertThat(response.topicId()).isEqualTo(1L);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.study.dto.StudyProgressResponseTest"`
Expected: FAIL

- [ ] **Step 3: StudyProgressRequest 구현**

```java
package com.junyoung.dashboard.domain.study.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudyProgressRequest(
        @NotNull Long topicId,
        @NotNull LocalDate studiedAt,
        @NotNull Integer minutes,
        @Size(max = 1000) String notes
) {
}
```

- [ ] **Step 4: StudyProgressResponse 구현**

```java
package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudyProgressResponse(
        Long id,
        Long topicId,
        LocalDate studiedAt,
        Integer minutes,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudyProgressResponse from(StudyProgress progress) {
        return new StudyProgressResponse(
                progress.getId(),
                progress.getTopic().getId(),
                progress.getStudiedAt(),
                progress.getMinutes(),
                progress.getNotes(),
                progress.getCreatedAt(),
                progress.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.study.dto.StudyProgressResponseTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add common/src
git commit -m "feat: StudyProgress 요청/응답 DTO 추가"
```

---

### Task 7: StudyProgressService

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/study/service/StudyProgressService.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/study/service/StudyProgressServiceTest.java`

**Interfaces:**
- Consumes: `StudyProgressRepository`(Task 5), `StudyTopicRepository`(Task 1), `StudyProgressRequest`/`StudyProgressResponse`(Task 6), `EntityNotFoundException`
- Produces: `class StudyProgressService`의 `create(StudyProgressRequest)`, `findByTopicId(Long)`, `findById(Long)`, `update(Long, StudyProgressRequest)`, `delete(Long)` — Task 8이 사용.

**중요 (Known Pitfall #5, hub의 실제 버그):** `update()`는 `request.topicId()`를 반드시 반영해야 한다. `topicId`가 바뀌면 `studyTopicRepository.findById(request.topicId())`로 새 topic을 조회하고(없으면 `EntityNotFoundException`), `StudyProgress.update(topic, studiedAt, minutes, notes)`를 호출한다. title/url만 반영하고 topic은 빼먹는 실수를 반복하지 않는다.

**중요 (Known Pitfall #4):** `create()`와 topic 변경 테스트는 `ArgumentCaptor<StudyProgress>`로 실제 `save()`/mutate에 전달된 `topic` 참조가 맞는지 검증한다.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressResponse;
import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.repository.StudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyProgressServiceTest {

    @Mock
    private StudyProgressRepository studyProgressRepository;

    @Mock
    private StudyTopicRepository studyTopicRepository;

    private StudyProgressService studyProgressService;

    @BeforeEach
    void setUp() {
        studyProgressService = new StudyProgressService(studyProgressRepository, studyTopicRepository);
    }

    @Test
    void createsProgressUnderExistingTopic() {
        StudyTopic topic = new StudyTopic("토익", null);
        StudyProgressRequest request = new StudyProgressRequest(1L, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이");
        when(studyTopicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(studyProgressRepository.save(any(StudyProgress.class)))
                .thenReturn(new StudyProgress(topic, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이"));

        StudyProgressResponse response = studyProgressService.create(request);

        assertThat(response.minutes()).isEqualTo(60);

        ArgumentCaptor<StudyProgress> captor = ArgumentCaptor.forClass(StudyProgress.class);
        verify(studyProgressRepository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isSameAs(topic);
    }

    @Test
    void throwsWhenTopicMissingOnCreate() {
        StudyProgressRequest request = new StudyProgressRequest(1L, LocalDate.of(2026, 9, 14), 60, null);
        when(studyTopicRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyProgressService.create(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void movesProgressToNewTopicOnUpdate() {
        StudyTopic oldTopic = new StudyTopic("토익", null);
        StudyTopic newTopic = new StudyTopic("알고리즘", null);
        StudyProgress progress = new StudyProgress(oldTopic, LocalDate.of(2026, 9, 14), 60, null);
        StudyProgressRequest request = new StudyProgressRequest(2L, LocalDate.of(2026, 9, 15), 90, "DP 복습");
        when(studyProgressRepository.findById(10L)).thenReturn(Optional.of(progress));
        when(studyTopicRepository.findById(2L)).thenReturn(Optional.of(newTopic));

        studyProgressService.update(10L, request);

        assertThat(progress.getTopic()).isSameAs(newTopic);
        assertThat(progress.getTopic()).isNotSameAs(oldTopic);
        assertThat(progress.getMinutes()).isEqualTo(90);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.service.StudyProgressServiceTest"`
Expected: FAIL

- [ ] **Step 3: StudyProgressService 구현**

```java
package com.junyoung.dashboard.domain.study.service;

import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressResponse;
import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.repository.StudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudyProgressService {

    private final StudyProgressRepository studyProgressRepository;
    private final StudyTopicRepository studyTopicRepository;

    public StudyProgressService(StudyProgressRepository studyProgressRepository, StudyTopicRepository studyTopicRepository) {
        this.studyProgressRepository = studyProgressRepository;
        this.studyTopicRepository = studyTopicRepository;
    }

    @Transactional
    public StudyProgressResponse create(StudyProgressRequest request) {
        StudyTopic topic = getTopicOrThrow(request.topicId());
        StudyProgress saved = studyProgressRepository.save(
                new StudyProgress(topic, request.studiedAt(), request.minutes(), request.notes()));
        return StudyProgressResponse.from(saved);
    }

    public List<StudyProgressResponse> findByTopicId(Long topicId) {
        return studyProgressRepository.findByTopicId(topicId).stream()
                .map(StudyProgressResponse::from)
                .toList();
    }

    public StudyProgressResponse findById(Long id) {
        return StudyProgressResponse.from(getOrThrow(id));
    }

    @Transactional
    public StudyProgressResponse update(Long id, StudyProgressRequest request) {
        StudyProgress progress = getOrThrow(id);
        StudyTopic topic = getTopicOrThrow(request.topicId());
        progress.update(topic, request.studiedAt(), request.minutes(), request.notes());
        return StudyProgressResponse.from(progress);
    }

    @Transactional
    public void delete(Long id) {
        studyProgressRepository.delete(getOrThrow(id));
    }

    private StudyProgress getOrThrow(Long id) {
        return studyProgressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("study progress " + id + " not found"));
    }

    private StudyTopic getTopicOrThrow(Long topicId) {
        return studyTopicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("study topic " + topicId + " not found"));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.service.StudyProgressServiceTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: StudyProgressService 추가"
```

---

### Task 8: StudyProgressController

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/study/controller/StudyProgressController.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/study/controller/StudyProgressControllerTest.java`

**Interfaces:**
- Consumes: `StudyProgressService`(Task 7), `ApiResponse`/`GlobalExceptionHandler`
- Produces: `POST /api/study/progresses`, `GET /api/study/progresses?topicId=`, `GET/PUT/DELETE /api/study/progresses/{id}` 엔드포인트.

**중요 (Known Pitfall #4):** POST/GET 테스트는 `jsonPath("$.data.topicId")`/`jsonPath("$.data[0].topicId")`를 반드시 검증한다 (hub에서 이 검증을 빠뜨렸다가 리뷰에서 3번 지적됨).

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressResponse;
import com.junyoung.dashboard.domain.study.service.StudyProgressService;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyProgressController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudyProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyProgressService studyProgressService;

    @Test
    void createsProgress() throws Exception {
        StudyProgressRequest request = new StudyProgressRequest(1L, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이");
        StudyProgressResponse response = new StudyProgressResponse(1L, 1L, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이",
                LocalDateTime.now(), LocalDateTime.now());
        when(studyProgressService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/study/progresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.minutes").value(60))
                .andExpect(jsonPath("$.data.topicId").value(1));
    }

    @Test
    void listsProgressesByTopic() throws Exception {
        when(studyProgressService.findByTopicId(1L)).thenReturn(List.of(
                new StudyProgressResponse(1L, 1L, LocalDate.of(2026, 9, 14), 60, null,
                        LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/study/progresses").param("topicId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].minutes").value(60))
                .andExpect(jsonPath("$.data[0].topicId").value(1));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.controller.StudyProgressControllerTest"`
Expected: FAIL

- [ ] **Step 3: StudyProgressController 구현**

```java
package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressResponse;
import com.junyoung.dashboard.domain.study.service.StudyProgressService;
import com.junyoung.dashboard.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/study/progresses")
public class StudyProgressController {

    private final StudyProgressService studyProgressService;

    public StudyProgressController(StudyProgressService studyProgressService) {
        this.studyProgressService = studyProgressService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudyProgressResponse> create(@Valid @RequestBody StudyProgressRequest request) {
        return ApiResponse.success(studyProgressService.create(request));
    }

    @GetMapping
    public ApiResponse<List<StudyProgressResponse>> findByTopicId(@RequestParam Long topicId) {
        return ApiResponse.success(studyProgressService.findByTopicId(topicId));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudyProgressResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(studyProgressService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudyProgressResponse> update(@PathVariable Long id, @Valid @RequestBody StudyProgressRequest request) {
        return ApiResponse.success(studyProgressService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studyProgressService.delete(id);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.study.controller.StudyProgressControllerTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: StudyProgressController 추가"
```

---

### Task 9: 통합 테스트 + PR

**Files:**
- Modify: `server/api/src/test/java/com/junyoung/dashboard/DashboardApplicationTests.java` (hub 도메인 때 이미 생성됨 — study 시나리오 테스트 메서드 추가)

**Interfaces:**
- Consumes: Task 1~8에서 만든 study 도메인 전체 스택

**참고:** `application.properties`는 이미 올바르게 구성되어 있다(datasource, ddl-auto=validate, flyway.enabled=true 모두 hub 도메인 때 완료). 이 태스크에서는 건드리지 않는다.

- [ ] **Step 1: 기존 통합 테스트에 study 시나리오 추가**

`DashboardApplicationTests`에 다음 테스트 메서드 추가:

```java
@Test
void createsAndFetchesStudyTopicEndToEnd() throws Exception {
    StudyTopicRequest request = new StudyTopicRequest("토익", "영어 공부");

    mockMvc.perform(post("/api/study/topics")
                    .header("X-API-KEY", "test-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").exists());

    mockMvc.perform(get("/api/study/topics")
                    .header("X-API-KEY", "test-api-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("토익"));
}
```
(필요한 import는 `com.junyoung.dashboard.domain.study.dto.StudyTopicRequest` 등 추가)

- [ ] **Step 2: 테스트 실행 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.DashboardApplicationTests"`
Expected: PASS

- [ ] **Step 3: 전체 빌드 확인**

Run: `./gradlew build` (server/ 디렉토리에서)
Expected: `BUILD SUCCESSFUL`, 모든 테스트 PASS

- [ ] **Step 4: 커밋**

```bash
git add api/src
git commit -m "test: study 도메인 엔드투엔드 통합 테스트 추가"
```

- [ ] **Step 5: 실제 Postgres 검증 (컨트롤러가 직접 진행)**

`docker compose up -d` + `./gradlew :api:bootRun`으로 실제 Postgres에 기동해 study API를 curl로 검증 (hub 도메인 때와 동일한 방식). V2 마이그레이션이 정상 적용되는지, 특히 topic_id 인덱스가 제대로 생성되는지 확인.

- [ ] **Step 6: 이슈 #4 체크박스 갱신 + PR**

```bash
gh issue view 4 --json body -q .body
# "study 도메인 CRUD" 체크박스를 [x]로 갱신
git push -u origin feature/domain-study-4
gh pr create --base main --head feature/domain-study-4 \
  --title "study 도메인 CRUD 구현" \
  --body "이슈 #4의 study 도메인 부분 완료. StudyTopic/StudyProgress CRUD, Flyway 마이그레이션(V2) 포함."
```

---

## 이 계획에 포함되지 않은 것

- `life`, `health`, `pknu`, `schedule`, `memo`, `user`, `reminder` 도메인 — 각자 `feature/domain-*` 브랜치의 별도 계획으로 진행
- 기존 `server/` 디렉토리 삭제 — 모든 도메인이 끝난 뒤 진행 (hub 도메인 때 이미 완료 표시됨, 재확인 불필요)
