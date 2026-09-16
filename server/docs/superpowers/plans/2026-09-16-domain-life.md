# life 도메인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `life` 도메인의 CRUD API를 구현한다. 두 개의 독립된 그룹으로 나뉜다: (A) `Habit` 1:N `HabitLog` (hub/study와 동일한 부모-자식 패턴), (B) `ReadingLog` (부모 없는 단독 CRUD 엔티티). hub/study 구현·리뷰에서 발견된 함정(아래 Known Pitfalls)을 처음부터 반영한다.

**Architecture:** `common` 모듈에 Entity+DTO, `api` 모듈에 Repository/Service/Controller를 둔다. Flyway V3(`habit`, `habit_log`, `reading_log` 테이블 전부 한 파일에)로 스키마를 만들고, API Key 인증 + `ApiResponse<T>` 래퍼 + `GlobalExceptionHandler`를 재사용한다.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway(Postgres), H2(테스트), Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md`

**Branch:** `feature/domain-life-4` (이슈 #4 하위 작업, 브랜치명에 이슈 번호 포함)

## Known Pitfalls (hub/study 도메인에서 발견, 이번 계획은 이미 반영함)

1. **Jackson은 3버전이다.** 테스트에서 ObjectMapper는 `tools.jackson.databind.ObjectMapper`를 import한다.
2. **테스트 어노테이션 패키지가 표준과 다르다.** `@WebMvcTest`/`@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest`는 `org.springframework.boot.data.jpa.test.autoconfigure`, `TestEntityManager`는 `org.springframework.boot.jpa.test.autoconfigure`. `api/build.gradle`에 필요한 스타터가 이미 다 있으니 **build.gradle을 건드리지 않는다.**
3. **common 모듈에 `spring-test`가 필요할 수 있다.** `ReflectionTestUtils`를 쓰는 DTO 테스트가 있다면 `common/build.gradle`에 이미 `testImplementation 'org.springframework:spring-test'`가 있다(study 도메인에서 추가됨) — 다시 추가할 필요 없음.
4. **테스트가 "이름은 검증을 약속하지만 실제로 검증 안 하는" 함정을 피해야 한다.** create/update 테스트는 `ArgumentCaptor`로 실제 전달된 값을 검증한다. FK 필드가 있는 응답 DTO 테스트는 `ReflectionTestUtils.setField`로 부모에 실제 id를 심어서 null==null 비교를 피한다.
5. **update()는 요청의 모든 필드를 실제로 반영해야 한다.** `HabitLogService.update()`는 `habitId`를 포함한 모든 필드를 실제로 반영한다 (엔티티의 `update()` 메서드 시그니처에 부모 참조를 포함시킨다).
6. **update()의 "새 부모가 없는 경우" 테스트를 처음부터 포함한다.** (study 최종 리뷰에서 지적됨 — happy-path move 테스트만 있고 not-found 테스트가 빠진 적이 있었다.) `HabitLogServiceTest`는 create-missing-parent, update-missing-parent, happy-path-move 세 가지를 전부 포함한다.
7. **숫자/범위가 있는 필드는 처음부터 검증한다.** (study 최종 리뷰에서 지적됨 — `minutes`에 범위 검증이 빠져있었다.) `ReadingLogRequest.rating`은 `@Min(1) @Max(5)`를 처음부터 포함한다.
8. **FK 컬럼에는 처음부터 인덱스를 만든다.**
9. **통합 테스트는 부모-자식 이동(move) 시나리오까지 포함한다.** (study 최종 리뷰에서 지적됨 — 부모 엔티티만 엔드투엔드 테스트가 있고 자식의 이동 시나리오는 수동 curl로만 검증됐었다.) `DashboardApplicationTests`에 HabitLog를 다른 Habit으로 옮기는 시나리오까지 넣는다.
10. **`server/` 디렉토리에서 `./gradlew` 실행.**

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- Entity는 `common`, Repository/Service/Controller는 `api`.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt) 상속.
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- API 경로 컨벤션: `/api/life/habits`, `/api/life/habit-logs`, `/api/life/reading-logs`.
- 스키마 관리는 Flyway. V1(hub), V2(study)는 이미 사용 중 — 이 계획은 V3을 쓴다. 운영은 `ddl-auto=validate`, 테스트는 `application-test.properties` 공유(이미 `spring.test.database.replace=none` 포함).
- 모든 @DataJpaTest는 @Import(JpaAuditingConfig.class) 선언.
- 이 계획을 시작하기 전에 `main`에 hub(PR #12), study(PR #13)가 이미 머지되어 있어야 한다.

## 엔티티 설계

- `Habit`(부모): `name`(필수, 100자), `description`(선택, 500자).
- `HabitLog`(자식): `habit`(FK, 필수), `performedAt`(LocalDate, 필수), `completed`(Boolean, 필수), `notes`(선택, 500자).
- `ReadingLog`(독립, 부모 없음): `title`(필수, 200자), `author`(선택, 100자), `startedAt`(LocalDate, 필수), `finishedAt`(LocalDate, 선택 — 아직 읽는 중이면 null), `rating`(Integer, 선택, 1~5), `notes`(선택, 1000자).

---

### Task 1: Habit 엔티티 + Flyway + Repository

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/life/entity/Habit.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/life/entity/HabitLog.java` (최소 버전, Task 5에서 완성)
- Create: `server/api/src/main/resources/db/migration/V3__life.sql` (이 태스크에서는 `habit`, `habit_log` 테이블만 작성 — `reading_log`는 Task 9에서 같은 파일에 추가)
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/life/repository/HabitRepository.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/life/repository/HabitRepositoryTest.java`

**Interfaces:**
- Consumes: `BaseEntity`
- Produces: `class Habit extends BaseEntity`(`getName()`, `getDescription()`, 생성자 `Habit(String name, String description)`, `void update(String name, String description)`), `interface HabitRepository extends JpaRepository<Habit, Long>`.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.Habit;
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
class HabitRepositoryTest {

    @Autowired
    private HabitRepository habitRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        Habit saved = habitRepository.save(new Habit("아침 스트레칭", "매일 아침 10분"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.life.repository.HabitRepositoryTest"`
Expected: FAIL

- [ ] **Step 3: Habit 엔티티 구현**

```java
package com.junyoung.dashboard.domain.life.entity;

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
@Table(name = "habit")
public class Habit extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HabitLog> logs = new ArrayList<>();

    public Habit(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
```

- [ ] **Step 4: HabitLog 최소 버전 작성 (컴파일용, Task 5에서 완성)**

```java
package com.junyoung.dashboard.domain.life.entity;

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
@Table(name = "habit_log")
public class HabitLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;
}
```

- [ ] **Step 5: HabitRepository 구현**

```java
package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitRepository extends JpaRepository<Habit, Long> {
}
```

- [ ] **Step 6: Flyway 마이그레이션 작성 (V3, habit/habit_log만 — reading_log는 Task 9에서 같은 파일에 추가)**

`server/api/src/main/resources/db/migration/V3__life.sql`:

```sql
CREATE TABLE habit (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE habit_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    habit_id BIGINT NOT NULL REFERENCES habit(id),
    performed_at DATE NOT NULL,
    completed BOOLEAN NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_habit_log_habit_id ON habit_log(habit_id);
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.life.repository.HabitRepositoryTest"`
Expected: PASS

- [ ] **Step 8: 커밋**

```bash
git add common/src api/src
git commit -m "feat: Habit 엔티티, Flyway 마이그레이션(V3), Repository 추가"
```

---

### Task 2: Habit DTO

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/life/dto/HabitRequest.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/life/dto/HabitResponse.java`
- Test: `server/common/src/test/java/com/junyoung/dashboard/domain/life/dto/HabitResponseTest.java`

**Interfaces:**
- Consumes: `Habit`(Task 1)
- Produces: `record HabitRequest(String name, String description)`, `record HabitResponse(Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt)`와 `HabitResponse.from(Habit)`.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.Habit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HabitResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        Habit habit = new Habit("아침 스트레칭", "매일 아침 10분");

        HabitResponse response = HabitResponse.from(habit);

        assertThat(response.name()).isEqualTo("아침 스트레칭");
        assertThat(response.description()).isEqualTo("매일 아침 10분");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.life.dto.HabitResponseTest"`

- [ ] **Step 3: HabitRequest 구현**

```java
package com.junyoung.dashboard.domain.life.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HabitRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
) {
}
```

- [ ] **Step 4: HabitResponse 구현**

```java
package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.Habit;

import java.time.LocalDateTime;

public record HabitResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HabitResponse from(Habit habit) {
        return new HabitResponse(
                habit.getId(),
                habit.getName(),
                habit.getDescription(),
                habit.getCreatedAt(),
                habit.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.life.dto.HabitResponseTest"`

- [ ] **Step 6: 커밋**

```bash
git add common/src
git commit -m "feat: Habit 요청/응답 DTO 추가"
```

---

### Task 3: HabitService

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/life/service/HabitService.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/life/service/HabitServiceTest.java`

**Interfaces:**
- Consumes: `HabitRepository`(Task 1), `HabitRequest`/`HabitResponse`(Task 2), `EntityNotFoundException`
- Produces: `class HabitService`의 `create`, `findAll`, `findById`, `update`, `delete`.

**중요 (Known Pitfall #4):** create 테스트는 `ArgumentCaptor`로 실제 save() 인자를 검증한다.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitResponse;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
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
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    private HabitService habitService;

    @BeforeEach
    void setUp() {
        habitService = new HabitService(habitRepository);
    }

    @Test
    void createsHabitUsingRequestFields() {
        HabitRequest request = new HabitRequest("아침 스트레칭", "매일 아침 10분");
        when(habitRepository.save(any(Habit.class)))
                .thenReturn(new Habit("아침 스트레칭", "매일 아침 10분"));

        HabitResponse response = habitService.create(request);

        assertThat(response.name()).isEqualTo("아침 스트레칭");

        ArgumentCaptor<Habit> captor = ArgumentCaptor.forClass(Habit.class);
        verify(habitRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo(request.name());
        assertThat(captor.getValue().getDescription()).isEqualTo(request.description());
    }

    @Test
    void throwsWhenHabitNotFound() {
        when(habitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: HabitService 구현**

```java
package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitResponse;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HabitService {

    private final HabitRepository habitRepository;

    public HabitService(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;
    }

    @Transactional
    public HabitResponse create(HabitRequest request) {
        Habit saved = habitRepository.save(new Habit(request.name(), request.description()));
        return HabitResponse.from(saved);
    }

    public List<HabitResponse> findAll() {
        return habitRepository.findAll().stream()
                .map(HabitResponse::from)
                .toList();
    }

    public HabitResponse findById(Long id) {
        return HabitResponse.from(getOrThrow(id));
    }

    @Transactional
    public HabitResponse update(Long id, HabitRequest request) {
        Habit habit = getOrThrow(id);
        habit.update(request.name(), request.description());
        return HabitResponse.from(habit);
    }

    @Transactional
    public void delete(Long id) {
        habitRepository.delete(getOrThrow(id));
    }

    private Habit getOrThrow(Long id) {
        return habitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("habit " + id + " not found"));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: HabitService 추가"
```

---

### Task 4: HabitController

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/life/controller/HabitController.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/life/controller/HabitControllerTest.java`

**Interfaces:**
- Consumes: `HabitService`(Task 3), `ApiResponse`/`GlobalExceptionHandler`
- Produces: `POST/GET/PUT/DELETE /api/life/habits`.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitResponse;
import com.junyoung.dashboard.domain.life.service.HabitService;
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

@WebMvcTest(HabitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HabitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HabitService habitService;

    @Test
    void createsHabit() throws Exception {
        HabitRequest request = new HabitRequest("아침 스트레칭", "매일 아침 10분");
        HabitResponse response = new HabitResponse(1L, "아침 스트레칭", "매일 아침 10분",
                LocalDateTime.now(), LocalDateTime.now());
        when(habitService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/life/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("아침 스트레칭"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        HabitRequest invalid = new HabitRequest("", null);

        mockMvc.perform(post("/api/life/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsHabits() throws Exception {
        when(habitService.findAll()).thenReturn(List.of(
                new HabitResponse(1L, "아침 스트레칭", null, LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/life/habits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("아침 스트레칭"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: HabitController 구현**

```java
package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitResponse;
import com.junyoung.dashboard.domain.life.service.HabitService;
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
@RequestMapping("/api/life/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HabitResponse> create(@Valid @RequestBody HabitRequest request) {
        return ApiResponse.success(habitService.create(request));
    }

    @GetMapping
    public ApiResponse<List<HabitResponse>> findAll() {
        return ApiResponse.success(habitService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<HabitResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(habitService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<HabitResponse> update(@PathVariable Long id, @Valid @RequestBody HabitRequest request) {
        return ApiResponse.success(habitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        habitService.delete(id);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: HabitController 추가"
```

---

### Task 5: HabitLog 완성 (엔티티/Repository)

**Files:**
- Modify: `server/common/src/main/java/com/junyoung/dashboard/domain/life/entity/HabitLog.java`
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/life/repository/HabitLogRepository.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/life/repository/HabitLogRepositoryTest.java`

**Interfaces:**
- Consumes: `Habit`(Task 1)
- Produces: `class HabitLog extends BaseEntity`(`getHabit()`, `getPerformedAt()`, `getCompleted()`, `getNotes()`, 생성자 `HabitLog(Habit, LocalDate, Boolean, String)`, **`void update(Habit habit, LocalDate performedAt, Boolean completed, String notes)`** — habit을 반드시 포함), `interface HabitLogRepository extends JpaRepository<HabitLog, Long>`의 `findByHabitId(Long)`.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
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
class HabitLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HabitLogRepository habitLogRepository;

    @Test
    void findsLogsByHabitId() {
        Habit habit = entityManager.persistAndFlush(new Habit("아침 스트레칭", null));
        entityManager.persistAndFlush(new HabitLog(habit, LocalDate.of(2026, 9, 16), true, "완료"));

        List<HabitLog> logs = habitLogRepository.findByHabitId(habit.getId());

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getCompleted()).isTrue();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: HabitLog 엔티티 완성**

```java
package com.junyoung.dashboard.domain.life.entity;

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
@Table(name = "habit_log")
public class HabitLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "performed_at", nullable = false)
    private LocalDate performedAt;

    @Column(nullable = false)
    private Boolean completed;

    @Column(length = 500)
    private String notes;

    public HabitLog(Habit habit, LocalDate performedAt, Boolean completed, String notes) {
        this.habit = habit;
        this.performedAt = performedAt;
        this.completed = completed;
        this.notes = notes;
    }

    public void update(Habit habit, LocalDate performedAt, Boolean completed, String notes) {
        this.habit = habit;
        this.performedAt = performedAt;
        this.completed = completed;
        this.notes = notes;
    }
}
```

- [ ] **Step 4: HabitLogRepository 구현**

```java
package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    List<HabitLog> findByHabitId(Long habitId);
}
```

- [ ] **Step 5: 테스트 통과 확인**

- [ ] **Step 6: 커밋**

```bash
git add common/src api/src
git commit -m "feat: HabitLog 엔티티 완성과 Repository 추가"
```

---

### Task 6: HabitLog DTO

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/life/dto/HabitLogRequest.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/life/dto/HabitLogResponse.java`
- Test: `server/common/src/test/java/com/junyoung/dashboard/domain/life/dto/HabitLogResponseTest.java`

**Interfaces:**
- Consumes: `HabitLog`, `Habit`(Task 5)
- Produces: `record HabitLogRequest(Long habitId, LocalDate performedAt, Boolean completed, String notes)`, `record HabitLogResponse(...)`와 `HabitLogResponse.from(HabitLog)`.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HabitLogResponseTest {

    @Test
    void mapsEntityFieldsIncludingHabitId() {
        Habit habit = new Habit("아침 스트레칭", null);
        ReflectionTestUtils.setField(habit, "id", 1L);
        HabitLog log = new HabitLog(habit, LocalDate.of(2026, 9, 16), true, "완료");

        HabitLogResponse response = HabitLogResponse.from(log);

        assertThat(response.completed()).isTrue();
        assertThat(response.notes()).isEqualTo("완료");
        assertThat(response.habitId()).isEqualTo(1L);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: HabitLogRequest 구현**

```java
package com.junyoung.dashboard.domain.life.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HabitLogRequest(
        @NotNull Long habitId,
        @NotNull LocalDate performedAt,
        @NotNull Boolean completed,
        @Size(max = 500) String notes
) {
}
```

- [ ] **Step 4: HabitLogResponse 구현**

```java
package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.HabitLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HabitLogResponse(
        Long id,
        Long habitId,
        LocalDate performedAt,
        Boolean completed,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HabitLogResponse from(HabitLog log) {
        return new HabitLogResponse(
                log.getId(),
                log.getHabit().getId(),
                log.getPerformedAt(),
                log.getCompleted(),
                log.getNotes(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

- [ ] **Step 6: 커밋**

```bash
git add common/src
git commit -m "feat: HabitLog 요청/응답 DTO 추가"
```

---

### Task 7: HabitLogService

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/life/service/HabitLogService.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/life/service/HabitLogServiceTest.java`

**Interfaces:**
- Consumes: `HabitLogRepository`(Task 5), `HabitRepository`(Task 1), `HabitLogRequest`/`HabitLogResponse`(Task 6), `EntityNotFoundException`
- Produces: `class HabitLogService`의 `create`, `findByHabitId`, `findById`, `update`, `delete`.

**중요 (Known Pitfall #5, #6):** `update()`는 `request.habitId()`를 반드시 반영한다 — `habitRepository.findById(request.habitId())`로 새 habit을 조회(없으면 `EntityNotFoundException`)하고 `HabitLog.update(habit, ...)`를 호출한다. 테스트는 create-missing-habit, update-missing-habit, happy-path-move **세 가지 전부** 포함한다.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.HabitLogResponse;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import com.junyoung.dashboard.domain.life.repository.HabitLogRepository;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
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
class HabitLogServiceTest {

    @Mock
    private HabitLogRepository habitLogRepository;

    @Mock
    private HabitRepository habitRepository;

    private HabitLogService habitLogService;

    @BeforeEach
    void setUp() {
        habitLogService = new HabitLogService(habitLogRepository, habitRepository);
    }

    @Test
    void createsLogUnderExistingHabit() {
        Habit habit = new Habit("아침 스트레칭", null);
        HabitLogRequest request = new HabitLogRequest(1L, LocalDate.of(2026, 9, 16), true, "완료");
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
        when(habitLogRepository.save(any(HabitLog.class)))
                .thenReturn(new HabitLog(habit, LocalDate.of(2026, 9, 16), true, "완료"));

        HabitLogResponse response = habitLogService.create(request);

        assertThat(response.completed()).isTrue();

        ArgumentCaptor<HabitLog> captor = ArgumentCaptor.forClass(HabitLog.class);
        verify(habitLogRepository).save(captor.capture());
        assertThat(captor.getValue().getHabit()).isSameAs(habit);
    }

    @Test
    void throwsWhenHabitMissingOnCreate() {
        HabitLogRequest request = new HabitLogRequest(1L, LocalDate.of(2026, 9, 16), true, null);
        when(habitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitLogService.create(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void movesLogToNewHabitOnUpdate() {
        Habit oldHabit = new Habit("아침 스트레칭", null);
        Habit newHabit = new Habit("저녁 독서", null);
        HabitLog log = new HabitLog(oldHabit, LocalDate.of(2026, 9, 16), true, null);
        HabitLogRequest request = new HabitLogRequest(2L, LocalDate.of(2026, 9, 17), false, "이동됨");
        when(habitLogRepository.findById(10L)).thenReturn(Optional.of(log));
        when(habitRepository.findById(2L)).thenReturn(Optional.of(newHabit));

        habitLogService.update(10L, request);

        assertThat(log.getHabit()).isSameAs(newHabit);
        assertThat(log.getHabit()).isNotSameAs(oldHabit);
        assertThat(log.getCompleted()).isFalse();
    }

    @Test
    void throwsWhenNewHabitMissingOnUpdate() {
        Habit oldHabit = new Habit("아침 스트레칭", null);
        HabitLog log = new HabitLog(oldHabit, LocalDate.of(2026, 9, 16), true, null);
        HabitLogRequest request = new HabitLogRequest(2L, LocalDate.of(2026, 9, 17), false, null);
        when(habitLogRepository.findById(10L)).thenReturn(Optional.of(log));
        when(habitRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitLogService.update(10L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: HabitLogService 구현**

```java
package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.HabitLogResponse;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import com.junyoung.dashboard.domain.life.repository.HabitLogRepository;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HabitLogService {

    private final HabitLogRepository habitLogRepository;
    private final HabitRepository habitRepository;

    public HabitLogService(HabitLogRepository habitLogRepository, HabitRepository habitRepository) {
        this.habitLogRepository = habitLogRepository;
        this.habitRepository = habitRepository;
    }

    @Transactional
    public HabitLogResponse create(HabitLogRequest request) {
        Habit habit = getHabitOrThrow(request.habitId());
        HabitLog saved = habitLogRepository.save(
                new HabitLog(habit, request.performedAt(), request.completed(), request.notes()));
        return HabitLogResponse.from(saved);
    }

    public List<HabitLogResponse> findByHabitId(Long habitId) {
        return habitLogRepository.findByHabitId(habitId).stream()
                .map(HabitLogResponse::from)
                .toList();
    }

    public HabitLogResponse findById(Long id) {
        return HabitLogResponse.from(getOrThrow(id));
    }

    @Transactional
    public HabitLogResponse update(Long id, HabitLogRequest request) {
        HabitLog log = getOrThrow(id);
        Habit habit = getHabitOrThrow(request.habitId());
        log.update(habit, request.performedAt(), request.completed(), request.notes());
        return HabitLogResponse.from(log);
    }

    @Transactional
    public void delete(Long id) {
        habitLogRepository.delete(getOrThrow(id));
    }

    private HabitLog getOrThrow(Long id) {
        return habitLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("habit log " + id + " not found"));
    }

    private Habit getHabitOrThrow(Long habitId) {
        return habitRepository.findById(habitId)
                .orElseThrow(() -> new EntityNotFoundException("habit " + habitId + " not found"));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: HabitLogService 추가"
```

---

### Task 8: HabitLogController

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/life/controller/HabitLogController.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/life/controller/HabitLogControllerTest.java`

**Interfaces:**
- Consumes: `HabitLogService`(Task 7), `ApiResponse`/`GlobalExceptionHandler`
- Produces: `POST /api/life/habit-logs`, `GET /api/life/habit-logs?habitId=`, `GET/PUT/DELETE /api/life/habit-logs/{id}`.

**중요 (Known Pitfall #4):** POST/GET 테스트는 `jsonPath("$.data.habitId")`/`jsonPath("$.data[0].habitId")`를 검증한다.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.HabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.HabitLogResponse;
import com.junyoung.dashboard.domain.life.service.HabitLogService;
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

@WebMvcTest(HabitLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HabitLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HabitLogService habitLogService;

    @Test
    void createsLog() throws Exception {
        HabitLogRequest request = new HabitLogRequest(1L, LocalDate.of(2026, 9, 16), true, "완료");
        HabitLogResponse response = new HabitLogResponse(1L, 1L, LocalDate.of(2026, 9, 16), true, "완료",
                LocalDateTime.now(), LocalDateTime.now());
        when(habitLogService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/life/habit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.habitId").value(1));
    }

    @Test
    void listsLogsByHabit() throws Exception {
        when(habitLogService.findByHabitId(1L)).thenReturn(List.of(
                new HabitLogResponse(1L, 1L, LocalDate.of(2026, 9, 16), true, null,
                        LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/life/habit-logs").param("habitId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].habitId").value(1));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: HabitLogController 구현**

```java
package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.HabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.HabitLogResponse;
import com.junyoung.dashboard.domain.life.service.HabitLogService;
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
@RequestMapping("/api/life/habit-logs")
public class HabitLogController {

    private final HabitLogService habitLogService;

    public HabitLogController(HabitLogService habitLogService) {
        this.habitLogService = habitLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HabitLogResponse> create(@Valid @RequestBody HabitLogRequest request) {
        return ApiResponse.success(habitLogService.create(request));
    }

    @GetMapping
    public ApiResponse<List<HabitLogResponse>> findByHabitId(@RequestParam Long habitId) {
        return ApiResponse.success(habitLogService.findByHabitId(habitId));
    }

    @GetMapping("/{id}")
    public ApiResponse<HabitLogResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(habitLogService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<HabitLogResponse> update(@PathVariable Long id, @Valid @RequestBody HabitLogRequest request) {
        return ApiResponse.success(habitLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        habitLogService.delete(id);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: HabitLogController 추가"
```

---

### Task 9: ReadingLog 엔티티 + Flyway + Repository

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/life/entity/ReadingLog.java`
- Modify: `server/api/src/main/resources/db/migration/V3__life.sql` (`reading_log` 테이블 추가)
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/life/repository/ReadingLogRepository.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/life/repository/ReadingLogRepositoryTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (부모 없음 — 독립 엔티티)
- Produces: `class ReadingLog extends BaseEntity`(`getTitle()`, `getAuthor()`, `getStartedAt()`, `getFinishedAt()`, `getRating()`, `getNotes()`, 생성자 `ReadingLog(String title, String author, LocalDate startedAt, LocalDate finishedAt, Integer rating, String notes)`, `void update(String title, String author, LocalDate startedAt, LocalDate finishedAt, Integer rating, String notes)`), `interface ReadingLogRepository extends JpaRepository<ReadingLog, Long>`.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class ReadingLogRepositoryTest {

    @Autowired
    private ReadingLogRepository readingLogRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        ReadingLog saved = readingLogRepository.save(
                new ReadingLog("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, null, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: ReadingLog 엔티티 구현**

```java
package com.junyoung.dashboard.domain.life.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "reading_log")
public class ReadingLog extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String author;

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "finished_at")
    private LocalDate finishedAt;

    private Integer rating;

    @Column(length = 1000)
    private String notes;

    public ReadingLog(String title, String author, LocalDate startedAt, LocalDate finishedAt, Integer rating, String notes) {
        this.title = title;
        this.author = author;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.rating = rating;
        this.notes = notes;
    }

    public void update(String title, String author, LocalDate startedAt, LocalDate finishedAt, Integer rating, String notes) {
        this.title = title;
        this.author = author;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.rating = rating;
        this.notes = notes;
    }
}
```

- [ ] **Step 4: ReadingLogRepository 구현**

```java
package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingLogRepository extends JpaRepository<ReadingLog, Long> {
}
```

- [ ] **Step 5: V3__life.sql에 reading_log 테이블 추가**

`server/api/src/main/resources/db/migration/V3__life.sql`의 기존 내용(Task 1에서 작성한 habit/habit_log) 끝에 아래를 추가:

```sql

CREATE TABLE reading_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    started_at DATE NOT NULL,
    finished_at DATE,
    rating INTEGER,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

**중요:** 이미 존재하는 `habit`/`habit_log` 테이블 정의는 절대 수정하지 않는다 — 이 파일은 이미 main에 머지되지 않은 로컬 커밋 상태이므로 단순히 끝에 추가만 한다. (V3__life.sql이 아직 main에 없다면 같은 브랜치의 Task 1 커밋에서 만든 파일이므로 이어서 수정하는 것이 맞다.)

- [ ] **Step 6: 테스트 통과 확인**

- [ ] **Step 7: 커밋**

```bash
git add common/src api/src
git commit -m "feat: ReadingLog 엔티티, Flyway 마이그레이션(V3 확장), Repository 추가"
```

---

### Task 10: ReadingLog DTO

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/life/dto/ReadingLogRequest.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/life/dto/ReadingLogResponse.java`
- Test: `server/common/src/test/java/com/junyoung/dashboard/domain/life/dto/ReadingLogResponseTest.java`

**Interfaces:**
- Consumes: `ReadingLog`(Task 9)
- Produces: `record ReadingLogRequest(String title, String author, LocalDate startedAt, LocalDate finishedAt, Integer rating, String notes)`, `record ReadingLogResponse(...)`와 `ReadingLogResponse.from(ReadingLog)`.

**중요 (Known Pitfall #7):** `rating`은 `@Min(1) @Max(5)`를 처음부터 포함한다 (선택 필드이므로 `@NotNull`은 없음 — null이거나 1~5).

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReadingLogResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        ReadingLog log = new ReadingLog("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, 5, "좋았음");

        ReadingLogResponse response = ReadingLogResponse.from(log);

        assertThat(response.title()).isEqualTo("클린 코드");
        assertThat(response.author()).isEqualTo("로버트 마틴");
        assertThat(response.rating()).isEqualTo(5);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: ReadingLogRequest 구현**

```java
package com.junyoung.dashboard.domain.life.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReadingLogRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 100) String author,
        @NotNull LocalDate startedAt,
        LocalDate finishedAt,
        @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String notes
) {
}
```

- [ ] **Step 4: ReadingLogResponse 구현**

```java
package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.ReadingLog;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReadingLogResponse(
        Long id,
        String title,
        String author,
        LocalDate startedAt,
        LocalDate finishedAt,
        Integer rating,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReadingLogResponse from(ReadingLog log) {
        return new ReadingLogResponse(
                log.getId(),
                log.getTitle(),
                log.getAuthor(),
                log.getStartedAt(),
                log.getFinishedAt(),
                log.getRating(),
                log.getNotes(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

- [ ] **Step 6: 커밋**

```bash
git add common/src
git commit -m "feat: ReadingLog 요청/응답 DTO 추가 (rating 범위 검증 포함)"
```

---

### Task 11: ReadingLogService

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/life/service/ReadingLogService.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/life/service/ReadingLogServiceTest.java`

**Interfaces:**
- Consumes: `ReadingLogRepository`(Task 9), `ReadingLogRequest`/`ReadingLogResponse`(Task 10), `EntityNotFoundException`
- Produces: `class ReadingLogService`의 `create`, `findAll`, `findById`, `update`, `delete`.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.ReadingLogRequest;
import com.junyoung.dashboard.domain.life.dto.ReadingLogResponse;
import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import com.junyoung.dashboard.domain.life.repository.ReadingLogRepository;
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
class ReadingLogServiceTest {

    @Mock
    private ReadingLogRepository readingLogRepository;

    private ReadingLogService readingLogService;

    @BeforeEach
    void setUp() {
        readingLogService = new ReadingLogService(readingLogRepository);
    }

    @Test
    void createsReadingLogUsingRequestFields() {
        ReadingLogRequest request = new ReadingLogRequest("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, null, null);
        when(readingLogRepository.save(any(ReadingLog.class)))
                .thenReturn(new ReadingLog("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, null, null));

        ReadingLogResponse response = readingLogService.create(request);

        assertThat(response.title()).isEqualTo("클린 코드");

        ArgumentCaptor<ReadingLog> captor = ArgumentCaptor.forClass(ReadingLog.class);
        verify(readingLogRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo(request.title());
        assertThat(captor.getValue().getAuthor()).isEqualTo(request.author());
    }

    @Test
    void throwsWhenReadingLogNotFound() {
        when(readingLogRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readingLogService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: ReadingLogService 구현**

```java
package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.ReadingLogRequest;
import com.junyoung.dashboard.domain.life.dto.ReadingLogResponse;
import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import com.junyoung.dashboard.domain.life.repository.ReadingLogRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReadingLogService {

    private final ReadingLogRepository readingLogRepository;

    public ReadingLogService(ReadingLogRepository readingLogRepository) {
        this.readingLogRepository = readingLogRepository;
    }

    @Transactional
    public ReadingLogResponse create(ReadingLogRequest request) {
        ReadingLog saved = readingLogRepository.save(new ReadingLog(
                request.title(), request.author(), request.startedAt(),
                request.finishedAt(), request.rating(), request.notes()));
        return ReadingLogResponse.from(saved);
    }

    public List<ReadingLogResponse> findAll() {
        return readingLogRepository.findAll().stream()
                .map(ReadingLogResponse::from)
                .toList();
    }

    public ReadingLogResponse findById(Long id) {
        return ReadingLogResponse.from(getOrThrow(id));
    }

    @Transactional
    public ReadingLogResponse update(Long id, ReadingLogRequest request) {
        ReadingLog log = getOrThrow(id);
        log.update(request.title(), request.author(), request.startedAt(),
                request.finishedAt(), request.rating(), request.notes());
        return ReadingLogResponse.from(log);
    }

    @Transactional
    public void delete(Long id) {
        readingLogRepository.delete(getOrThrow(id));
    }

    private ReadingLog getOrThrow(Long id) {
        return readingLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("reading log " + id + " not found"));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: ReadingLogService 추가"
```

---

### Task 12: ReadingLogController

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/life/controller/ReadingLogController.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/life/controller/ReadingLogControllerTest.java`

**Interfaces:**
- Consumes: `ReadingLogService`(Task 11), `ApiResponse`/`GlobalExceptionHandler`
- Produces: `POST/GET/PUT/DELETE /api/life/reading-logs`.

**중요 (Known Pitfall #7):** rating 검증 실패(0 또는 6) 테스트를 포함한다.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.ReadingLogRequest;
import com.junyoung.dashboard.domain.life.dto.ReadingLogResponse;
import com.junyoung.dashboard.domain.life.service.ReadingLogService;
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

@WebMvcTest(ReadingLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReadingLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReadingLogService readingLogService;

    @Test
    void createsReadingLog() throws Exception {
        ReadingLogRequest request = new ReadingLogRequest("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, 5, null);
        ReadingLogResponse response = new ReadingLogResponse(1L, "클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, 5, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(readingLogService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/life/reading-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("클린 코드"));
    }

    @Test
    void rejectsRatingOutOfRange() throws Exception {
        ReadingLogRequest invalid = new ReadingLogRequest("클린 코드", null, LocalDate.of(2026, 9, 1), null, 6, null);

        mockMvc.perform(post("/api/life/reading-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsReadingLogs() throws Exception {
        when(readingLogService.findAll()).thenReturn(List.of(
                new ReadingLogResponse(1L, "클린 코드", null, LocalDate.of(2026, 9, 1), null, null, null,
                        LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/life/reading-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("클린 코드"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

- [ ] **Step 3: ReadingLogController 구현**

```java
package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.ReadingLogRequest;
import com.junyoung.dashboard.domain.life.dto.ReadingLogResponse;
import com.junyoung.dashboard.domain.life.service.ReadingLogService;
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
@RequestMapping("/api/life/reading-logs")
public class ReadingLogController {

    private final ReadingLogService readingLogService;

    public ReadingLogController(ReadingLogService readingLogService) {
        this.readingLogService = readingLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReadingLogResponse> create(@Valid @RequestBody ReadingLogRequest request) {
        return ApiResponse.success(readingLogService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ReadingLogResponse>> findAll() {
        return ApiResponse.success(readingLogService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ReadingLogResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(readingLogService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReadingLogResponse> update(@PathVariable Long id, @Valid @RequestBody ReadingLogRequest request) {
        return ApiResponse.success(readingLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        readingLogService.delete(id);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: ReadingLogController 추가"
```

---

### Task 13: 통합 테스트 (Habit/HabitLog 이동 시나리오 포함) + PR

**Files:**
- Modify: `server/api/src/test/java/com/junyoung/dashboard/DashboardApplicationTests.java`

**Interfaces:**
- Consumes: Task 1~12에서 만든 life 도메인 전체 스택

**참고:** `application.properties`는 이미 올바르게 구성되어 있다. 이 태스크에서는 건드리지 않는다.

**중요 (Known Pitfall #9):** HabitLog를 다른 Habit으로 이동하는 시나리오까지 통합 테스트에 포함한다 (study 도메인 최종 리뷰에서 지적된 "부모만 테스트하고 자식의 이동은 수동 검증만 함" 문제를 재발시키지 않기 위함).

- [ ] **Step 1: 기존 통합 테스트에 life 시나리오 추가**

`DashboardApplicationTests`에 다음 테스트 메서드 추가:

```java
@Test
void movesHabitLogBetweenHabitsEndToEnd() throws Exception {
    HabitRequest habitARequest = new HabitRequest("아침 스트레칭", null);
    String habitAResponse = mockMvc.perform(post("/api/life/habits")
                    .header("X-API-KEY", "test-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(habitARequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    Long habitAId = objectMapper.readTree(habitAResponse).get("data").get("id").asLong();

    HabitRequest habitBRequest = new HabitRequest("저녁 독서", null);
    String habitBResponse = mockMvc.perform(post("/api/life/habits")
                    .header("X-API-KEY", "test-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(habitBRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    Long habitBId = objectMapper.readTree(habitBResponse).get("data").get("id").asLong();

    HabitLogRequest createRequest = new HabitLogRequest(habitAId, LocalDate.of(2026, 9, 16), true, null);
    String logResponse = mockMvc.perform(post("/api/life/habit-logs")
                    .header("X-API-KEY", "test-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.habitId").value(habitAId))
            .andReturn().getResponse().getContentAsString();
    Long logId = objectMapper.readTree(logResponse).get("data").get("id").asLong();

    HabitLogRequest moveRequest = new HabitLogRequest(habitBId, LocalDate.of(2026, 9, 17), false, "이동됨");
    mockMvc.perform(put("/api/life/habit-logs/" + logId)
                    .header("X-API-KEY", "test-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(moveRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.habitId").value(habitBId));
}

@Test
void createsAndFetchesReadingLogEndToEnd() throws Exception {
    ReadingLogRequest request = new ReadingLogRequest("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, 5, null);

    mockMvc.perform(post("/api/life/reading-logs")
                    .header("X-API-KEY", "test-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").exists());

    mockMvc.perform(get("/api/life/reading-logs")
                    .header("X-API-KEY", "test-api-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("클린 코드"));
}
```
(필요한 import 추가: `com.junyoung.dashboard.domain.life.dto.HabitRequest`, `HabitLogRequest`, `ReadingLogRequest`, `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put`)

- [ ] **Step 2: 테스트 실행 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.DashboardApplicationTests"`
Expected: PASS

- [ ] **Step 3: 전체 빌드 확인**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add api/src
git commit -m "test: life 도메인 엔드투엔드 통합 테스트 추가 (habit-log 이동 시나리오 포함)"
```

- [ ] **Step 5: 실제 Postgres 검증 (컨트롤러가 직접 진행)**

`docker compose up -d` + `./gradlew :api:bootRun`으로 실제 Postgres에 기동해 life API를 curl로 검증. V3 마이그레이션(habit, habit_log, reading_log 3개 테이블 + 인덱스)이 정상 적용되는지 확인. 로컬 볼륨에 이전 버전 마이그레이션이 이미 적용돼 있어 체크섬 불일치가 나면 `docker compose down -v`로 리셋 후 재검증 (study 도메인 때와 동일 패턴).

- [ ] **Step 6: 이슈 #4 체크박스 갱신 + PR**

```bash
git push -u origin feature/domain-life-4
gh pr create --base main --head feature/domain-life-4 \
  --title "life 도메인 CRUD 구현" \
  --body "이슈 #4의 life 도메인 부분 완료. Habit/HabitLog, ReadingLog CRUD, Flyway 마이그레이션(V3) 포함."
```

---

## 이 계획에 포함되지 않은 것

- `health`, `pknu`, `schedule`, `memo`, `user`, `reminder` 도메인 — 각자 `feature/domain-*` 브랜치의 별도 계획으로 진행
