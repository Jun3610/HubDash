# hub 도메인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `hub` 도메인(HubCategory 1:N HubLink)의 CRUD API를 끝까지 구현해, 이후 study/life/health/pknu/schedule/memo/user/reminder 도메인이 따라올 패턴을 확정한다.

**Architecture:** `common` 모듈에 Entity+DTO, `api` 모듈에 Repository/Service/Controller를 둔다. Flyway로 `hub_category`, `hub_link` 테이블을 만들고, API Key 인증 + `ApiResponse<T>` 래퍼 + `GlobalExceptionHandler`를 그대로 재사용한다.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Flyway(Postgres), H2(테스트), Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md`

**Branch:** `feature/domain-hub` (`main`에서 새로 생성 — `main`에는 `feature/phase2-scaffolding`이 이미 머지되어 `common`/`api` 모듈과 `global` 공통 요소가 존재한다고 가정)

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- Entity는 `common`, Repository/Service/Controller는 `api`.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt) 상속.
- Entity는 컨트롤러 밖으로 노출 금지 — Request/Response DTO로만 통신.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- API 경로 컨벤션: `/api/hub/...`.
- 스키마 관리는 Flyway. 운영은 `ddl-auto=validate`, 테스트는 `spring.flyway.enabled=false` + `ddl-auto=create-drop` + H2.
- 이 계획을 시작하기 전에 `main`에 `feature/phase2-scaffolding`이 머지되어 있어야 한다 (`BaseEntity`, `ApiResponse`, `GlobalExceptionHandler`, `ApiKeyAuthFilter`가 이미 존재).
- 모든 @DataJpaTest는 @Import(JpaAuditingConfig.class)를 함께 선언해야 createdAt/updatedAt이 채워진다 (JpaAuditingConfig는 api 모듈의 평범한 @Configuration이라 @DataJpaTest의 슬라이스에 기본 포함되지 않음).
- Gradle 멀티모듈(common/api)은 레포 루트가 아니라 `server/` 하위에 있다 (`server/settings.gradle`, `server/common/`, `server/api/`). 이 계획의 모든 `./gradlew ...` 명령은 레포 루트가 아니라 `server/` 디렉토리 안에서 실행해야 한다.

---

### Task 1: HubCategory 엔티티 + Flyway + Repository

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/hub/entity/HubCategory.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/hub/entity/HubLink.java` (이 태스크에서는 컴파일용 최소 버전만, Task 5에서 완성)
- Create: `server/api/src/main/resources/db/migration/V1__init.sql`
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/hub/repository/HubCategoryRepository.java`
- Create: `server/api/src/test/resources/application-test.properties`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/hub/repository/HubCategoryRepositoryTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (scaffolding 브랜치, `main`에 이미 머지됨)
- Produces: `class HubCategory extends BaseEntity`(`getName()`, `getDescription()`, 생성자 `HubCategory(String name, String description)`, `void update(String name, String description)`), `interface HubCategoryRepository extends JpaRepository<HubCategory, Long>` — Task 3(Service)이 사용.

- [ ] **Step 1: 테스트 프로파일 설정 작성**

`server/api/src/test/resources/application-test.properties`:

```properties
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
app.api-key=test-api-key
```

- [ ] **Step 2: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
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
class HubCategoryRepositoryTest {

    @Autowired
    private HubCategoryRepository hubCategoryRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        HubCategory saved = hubCategoryRepository.save(new HubCategory("CI/CD", "빌드 파이프라인 문서"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
```

- [ ] **Step 3: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.repository.HubCategoryRepositoryTest"`
Expected: FAIL (컴파일 에러 — `HubCategory`, `HubCategoryRepository`가 없음)

- [ ] **Step 4: HubCategory 엔티티 구현**

```java
package com.junyoung.dashboard.domain.hub.entity;

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
@Table(name = "hub_category")
public class HubCategory extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HubLink> links = new ArrayList<>();

    public HubCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
```

- [ ] **Step 5: HubLink 최소 버전 작성 (컴파일용, Task 5에서 완성)**

```java
package com.junyoung.dashboard.domain.hub.entity;

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
@Table(name = "hub_link")
public class HubLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private HubCategory category;
}
```

- [ ] **Step 6: HubCategoryRepository 구현**

```java
package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubCategoryRepository extends JpaRepository<HubCategory, Long> {
}
```

- [ ] **Step 7: Flyway 마이그레이션 작성**

`server/api/src/main/resources/db/migration/V1__init.sql`:

```sql
CREATE TABLE hub_category (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE hub_link (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES hub_category(id),
    title VARCHAR(200) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

- [ ] **Step 8: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.repository.HubCategoryRepositoryTest"`
Expected: PASS

- [ ] **Step 9: 커밋**

```bash
git add common/src api/src
git commit -m "feat: HubCategory 엔티티, Flyway 마이그레이션, Repository 추가"
```

---

### Task 2: HubCategory DTO

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/hub/dto/HubCategoryRequest.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/hub/dto/HubCategoryResponse.java`
- Test: `server/common/src/test/java/com/junyoung/dashboard/domain/hub/dto/HubCategoryResponseTest.java`

**Interfaces:**
- Consumes: `HubCategory`(Task 1)
- Produces: `record HubCategoryRequest(String name, String description)`, `record HubCategoryResponse(Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt)`와 `HubCategoryResponse.from(HubCategory)` — Task 3(Service)이 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.hub.dto;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HubCategoryResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        HubCategory category = new HubCategory("CI/CD", "빌드 파이프라인 문서");

        HubCategoryResponse response = HubCategoryResponse.from(category);

        assertThat(response.name()).isEqualTo("CI/CD");
        assertThat(response.description()).isEqualTo("빌드 파이프라인 문서");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.hub.dto.HubCategoryResponseTest"`
Expected: FAIL (컴파일 에러 — `HubCategoryResponse`가 없음)

- [ ] **Step 3: HubCategoryRequest 구현**

```java
package com.junyoung.dashboard.domain.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HubCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
) {
}
```

- [ ] **Step 4: HubCategoryResponse 구현**

```java
package com.junyoung.dashboard.domain.hub.dto;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;

import java.time.LocalDateTime;

public record HubCategoryResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HubCategoryResponse from(HubCategory category) {
        return new HubCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.hub.dto.HubCategoryResponseTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add common/src
git commit -m "feat: HubCategory 요청/응답 DTO 추가"
```

---

### Task 3: HubCategoryService

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/hub/service/HubCategoryService.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/hub/service/HubCategoryServiceTest.java`

**Interfaces:**
- Consumes: `HubCategoryRepository`(Task 1), `HubCategoryRequest`/`HubCategoryResponse`(Task 2), `EntityNotFoundException`(scaffolding 브랜치)
- Produces: `class HubCategoryService`의 `create(HubCategoryRequest)`, `findAll()`, `findById(Long)`, `update(Long, HubCategoryRequest)`, `delete(Long)` — Task 4(Controller)가 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryResponse;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubCategoryServiceTest {

    @Mock
    private HubCategoryRepository hubCategoryRepository;

    private HubCategoryService hubCategoryService;

    @BeforeEach
    void setUp() {
        hubCategoryService = new HubCategoryService(hubCategoryRepository);
    }

    @Test
    void createsCategoryAndReturnsResponse() {
        HubCategoryRequest request = new HubCategoryRequest("CI/CD", "빌드 파이프라인 문서");
        when(hubCategoryRepository.save(any(HubCategory.class)))
                .thenReturn(new HubCategory("CI/CD", "빌드 파이프라인 문서"));

        HubCategoryResponse response = hubCategoryService.create(request);

        assertThat(response.name()).isEqualTo("CI/CD");
        assertThat(response.description()).isEqualTo("빌드 파이프라인 문서");
    }

    @Test
    void throwsWhenCategoryNotFound() {
        when(hubCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hubCategoryService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.service.HubCategoryServiceTest"`
Expected: FAIL (컴파일 에러 — `HubCategoryService`가 없음)

- [ ] **Step 3: HubCategoryService 구현**

```java
package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryResponse;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HubCategoryService {

    private final HubCategoryRepository hubCategoryRepository;

    public HubCategoryService(HubCategoryRepository hubCategoryRepository) {
        this.hubCategoryRepository = hubCategoryRepository;
    }

    @Transactional
    public HubCategoryResponse create(HubCategoryRequest request) {
        HubCategory saved = hubCategoryRepository.save(
                new HubCategory(request.name(), request.description()));
        return HubCategoryResponse.from(saved);
    }

    public List<HubCategoryResponse> findAll() {
        return hubCategoryRepository.findAll().stream()
                .map(HubCategoryResponse::from)
                .toList();
    }

    public HubCategoryResponse findById(Long id) {
        return HubCategoryResponse.from(getOrThrow(id));
    }

    @Transactional
    public HubCategoryResponse update(Long id, HubCategoryRequest request) {
        HubCategory category = getOrThrow(id);
        category.update(request.name(), request.description());
        return HubCategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        hubCategoryRepository.delete(getOrThrow(id));
    }

    private HubCategory getOrThrow(Long id) {
        return hubCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("hub category " + id + " not found"));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.service.HubCategoryServiceTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: HubCategoryService 추가"
```

---

### Task 4: HubCategoryController

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/hub/controller/HubCategoryController.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/hub/controller/HubCategoryControllerTest.java`

**Interfaces:**
- Consumes: `HubCategoryService`(Task 3), `ApiResponse`/`GlobalExceptionHandler`(scaffolding 브랜치)
- Produces: `POST/GET/PUT/DELETE /api/hub/categories` 엔드포인트.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.hub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryResponse;
import com.junyoung.dashboard.domain.hub.service.HubCategoryService;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HubCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HubCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HubCategoryService hubCategoryService;

    @Test
    void createsCategory() throws Exception {
        HubCategoryRequest request = new HubCategoryRequest("CI/CD", "빌드 파이프라인 문서");
        HubCategoryResponse response = new HubCategoryResponse(1L, "CI/CD", "빌드 파이프라인 문서",
                LocalDateTime.now(), LocalDateTime.now());
        when(hubCategoryService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/hub/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("CI/CD"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        HubCategoryRequest invalid = new HubCategoryRequest("", null);

        mockMvc.perform(post("/api/hub/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsCategories() throws Exception {
        when(hubCategoryService.findAll()).thenReturn(List.of(
                new HubCategoryResponse(1L, "CI/CD", null, LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/hub/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("CI/CD"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.controller.HubCategoryControllerTest"`
Expected: FAIL (컴파일 에러 — `HubCategoryController`가 없음)

- [ ] **Step 3: HubCategoryController 구현**

```java
package com.junyoung.dashboard.domain.hub.controller;

import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryResponse;
import com.junyoung.dashboard.domain.hub.service.HubCategoryService;
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
@RequestMapping("/api/hub/categories")
public class HubCategoryController {

    private final HubCategoryService hubCategoryService;

    public HubCategoryController(HubCategoryService hubCategoryService) {
        this.hubCategoryService = hubCategoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HubCategoryResponse> create(@Valid @RequestBody HubCategoryRequest request) {
        return ApiResponse.success(hubCategoryService.create(request));
    }

    @GetMapping
    public ApiResponse<List<HubCategoryResponse>> findAll() {
        return ApiResponse.success(hubCategoryService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<HubCategoryResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(hubCategoryService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<HubCategoryResponse> update(@PathVariable Long id, @Valid @RequestBody HubCategoryRequest request) {
        return ApiResponse.success(hubCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hubCategoryService.delete(id);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.controller.HubCategoryControllerTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: HubCategoryController 추가"
```

---

### Task 5: HubLink 완성 (엔티티/Repository)

**Files:**
- Modify: `server/common/src/main/java/com/junyoung/dashboard/domain/hub/entity/HubLink.java` (Task 1의 최소 버전을 완성)
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/hub/repository/HubLinkRepository.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/hub/repository/HubLinkRepositoryTest.java`

**Interfaces:**
- Consumes: `HubCategory`(Task 1)
- Produces: `class HubLink extends BaseEntity`(`getCategory()`, `getTitle()`, `getUrl()`, `getDescription()`, 생성자 `HubLink(HubCategory, String title, String url, String description)`, `void update(String title, String url, String description)`), `interface HubLinkRepository extends JpaRepository<HubLink, Long>`의 `findByCategoryId(Long)` — Task 7(Service)이 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class HubLinkRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HubLinkRepository hubLinkRepository;

    @Test
    void findsLinksByCategoryId() {
        HubCategory category = entityManager.persistAndFlush(new HubCategory("CI/CD", null));
        entityManager.persistAndFlush(new HubLink(category, "Docker 문서", "https://example.com/docker", null));

        List<HubLink> links = hubLinkRepository.findByCategoryId(category.getId());

        assertThat(links).hasSize(1);
        assertThat(links.get(0).getTitle()).isEqualTo("Docker 문서");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.repository.HubLinkRepositoryTest"`
Expected: FAIL (컴파일 에러 — `HubLink`에 생성자/메서드가 없고 `HubLinkRepository`도 없음)

- [ ] **Step 3: HubLink 엔티티 완성**

```java
package com.junyoung.dashboard.domain.hub.entity;

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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "hub_link")
public class HubLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private HubCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(length = 500)
    private String description;

    public HubLink(HubCategory category, String title, String url, String description) {
        this.category = category;
        this.title = title;
        this.url = url;
        this.description = description;
    }

    public void update(String title, String url, String description) {
        this.title = title;
        this.url = url;
        this.description = description;
    }
}
```

- [ ] **Step 4: HubLinkRepository 구현**

```java
package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HubLinkRepository extends JpaRepository<HubLink, Long> {
    List<HubLink> findByCategoryId(Long categoryId);
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.repository.HubLinkRepositoryTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add common/src api/src
git commit -m "feat: HubLink 엔티티 완성과 Repository 추가"
```

---

### Task 6: HubLink DTO

**Files:**
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/hub/dto/HubLinkRequest.java`
- Create: `server/common/src/main/java/com/junyoung/dashboard/domain/hub/dto/HubLinkResponse.java`
- Test: `server/common/src/test/java/com/junyoung/dashboard/domain/hub/dto/HubLinkResponseTest.java`

**Interfaces:**
- Consumes: `HubLink`, `HubCategory`(Task 5)
- Produces: `record HubLinkRequest(Long categoryId, String title, String url, String description)`, `record HubLinkResponse(...)`와 `HubLinkResponse.from(HubLink)` — Task 7(Service)이 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.hub.dto;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HubLinkResponseTest {

    @Test
    void mapsEntityFieldsIncludingCategoryId() {
        HubCategory category = new HubCategory("CI/CD", null);
        HubLink link = new HubLink(category, "Docker 문서", "https://example.com/docker", null);

        HubLinkResponse response = HubLinkResponse.from(link);

        assertThat(response.title()).isEqualTo("Docker 문서");
        assertThat(response.url()).isEqualTo("https://example.com/docker");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.hub.dto.HubLinkResponseTest"`
Expected: FAIL (컴파일 에러 — `HubLinkResponse`가 없음)

- [ ] **Step 3: HubLinkRequest 구현**

```java
package com.junyoung.dashboard.domain.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HubLinkRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 1000) String url,
        @Size(max = 500) String description
) {
}
```

- [ ] **Step 4: HubLinkResponse 구현**

```java
package com.junyoung.dashboard.domain.hub.dto;

import com.junyoung.dashboard.domain.hub.entity.HubLink;

import java.time.LocalDateTime;

public record HubLinkResponse(
        Long id,
        Long categoryId,
        String title,
        String url,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HubLinkResponse from(HubLink link) {
        return new HubLinkResponse(
                link.getId(),
                link.getCategory().getId(),
                link.getTitle(),
                link.getUrl(),
                link.getDescription(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.domain.hub.dto.HubLinkResponseTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add common/src
git commit -m "feat: HubLink 요청/응답 DTO 추가"
```

---

### Task 7: HubLinkService

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/hub/service/HubLinkService.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/hub/service/HubLinkServiceTest.java`

**Interfaces:**
- Consumes: `HubLinkRepository`(Task 5), `HubCategoryRepository`(Task 1), `HubLinkRequest`/`HubLinkResponse`(Task 6), `EntityNotFoundException`(scaffolding 브랜치)
- Produces: `class HubLinkService`의 `create(HubLinkRequest)`, `findByCategoryId(Long)`, `findById(Long)`, `update(Long, HubLinkRequest)`, `delete(Long)` — Task 8(Controller)이 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.dto.HubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.HubLinkResponse;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubLinkServiceTest {

    @Mock
    private HubLinkRepository hubLinkRepository;

    @Mock
    private HubCategoryRepository hubCategoryRepository;

    private HubLinkService hubLinkService;

    @BeforeEach
    void setUp() {
        hubLinkService = new HubLinkService(hubLinkRepository, hubCategoryRepository);
    }

    @Test
    void createsLinkUnderExistingCategory() {
        HubCategory category = new HubCategory("CI/CD", null);
        HubLinkRequest request = new HubLinkRequest(1L, "Docker 문서", "https://example.com/docker", null);
        when(hubCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(hubLinkRepository.save(any(HubLink.class)))
                .thenReturn(new HubLink(category, "Docker 문서", "https://example.com/docker", null));

        HubLinkResponse response = hubLinkService.create(request);

        assertThat(response.title()).isEqualTo("Docker 문서");
    }

    @Test
    void throwsWhenCategoryMissing() {
        HubLinkRequest request = new HubLinkRequest(1L, "Docker 문서", "https://example.com/docker", null);
        when(hubCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hubLinkService.create(request))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.service.HubLinkServiceTest"`
Expected: FAIL (컴파일 에러 — `HubLinkService`가 없음)

- [ ] **Step 3: HubLinkService 구현**

```java
package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.dto.HubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.HubLinkResponse;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HubLinkService {

    private final HubLinkRepository hubLinkRepository;
    private final HubCategoryRepository hubCategoryRepository;

    public HubLinkService(HubLinkRepository hubLinkRepository, HubCategoryRepository hubCategoryRepository) {
        this.hubLinkRepository = hubLinkRepository;
        this.hubCategoryRepository = hubCategoryRepository;
    }

    @Transactional
    public HubLinkResponse create(HubLinkRequest request) {
        HubCategory category = hubCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("hub category " + request.categoryId() + " not found"));
        HubLink saved = hubLinkRepository.save(
                new HubLink(category, request.title(), request.url(), request.description()));
        return HubLinkResponse.from(saved);
    }

    public List<HubLinkResponse> findByCategoryId(Long categoryId) {
        return hubLinkRepository.findByCategoryId(categoryId).stream()
                .map(HubLinkResponse::from)
                .toList();
    }

    public HubLinkResponse findById(Long id) {
        return HubLinkResponse.from(getOrThrow(id));
    }

    @Transactional
    public HubLinkResponse update(Long id, HubLinkRequest request) {
        HubLink link = getOrThrow(id);
        link.update(request.title(), request.url(), request.description());
        return HubLinkResponse.from(link);
    }

    @Transactional
    public void delete(Long id) {
        hubLinkRepository.delete(getOrThrow(id));
    }

    private HubLink getOrThrow(Long id) {
        return hubLinkRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("hub link " + id + " not found"));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.service.HubLinkServiceTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: HubLinkService 추가"
```

---

### Task 8: HubLinkController

**Files:**
- Create: `server/api/src/main/java/com/junyoung/dashboard/domain/hub/controller/HubLinkController.java`
- Test: `server/api/src/test/java/com/junyoung/dashboard/domain/hub/controller/HubLinkControllerTest.java`

**Interfaces:**
- Consumes: `HubLinkService`(Task 7), `ApiResponse`/`GlobalExceptionHandler`(scaffolding 브랜치)
- Produces: `POST /api/hub/links`, `GET /api/hub/links?categoryId=`, `GET/PUT/DELETE /api/hub/links/{id}` 엔드포인트.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.domain.hub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junyoung.dashboard.domain.hub.dto.HubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.HubLinkResponse;
import com.junyoung.dashboard.domain.hub.service.HubLinkService;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HubLinkController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HubLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HubLinkService hubLinkService;

    @Test
    void createsLink() throws Exception {
        HubLinkRequest request = new HubLinkRequest(1L, "Docker 문서", "https://example.com/docker", null);
        HubLinkResponse response = new HubLinkResponse(1L, 1L, "Docker 문서", "https://example.com/docker", null,
                LocalDateTime.now(), LocalDateTime.now());
        when(hubLinkService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/hub/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Docker 문서"));
    }

    @Test
    void listsLinksByCategory() throws Exception {
        when(hubLinkService.findByCategoryId(1L)).thenReturn(List.of(
                new HubLinkResponse(1L, 1L, "Docker 문서", "https://example.com/docker", null,
                        LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/hub/links").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Docker 문서"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.controller.HubLinkControllerTest"`
Expected: FAIL (컴파일 에러 — `HubLinkController`가 없음)

- [ ] **Step 3: HubLinkController 구현**

```java
package com.junyoung.dashboard.domain.hub.controller;

import com.junyoung.dashboard.domain.hub.dto.HubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.HubLinkResponse;
import com.junyoung.dashboard.domain.hub.service.HubLinkService;
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
@RequestMapping("/api/hub/links")
public class HubLinkController {

    private final HubLinkService hubLinkService;

    public HubLinkController(HubLinkService hubLinkService) {
        this.hubLinkService = hubLinkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HubLinkResponse> create(@Valid @RequestBody HubLinkRequest request) {
        return ApiResponse.success(hubLinkService.create(request));
    }

    @GetMapping
    public ApiResponse<List<HubLinkResponse>> findByCategoryId(@RequestParam Long categoryId) {
        return ApiResponse.success(hubLinkService.findByCategoryId(categoryId));
    }

    @GetMapping("/{id}")
    public ApiResponse<HubLinkResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(hubLinkService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<HubLinkResponse> update(@PathVariable Long id, @Valid @RequestBody HubLinkRequest request) {
        return ApiResponse.success(hubLinkService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hubLinkService.delete(id);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.domain.hub.controller.HubLinkControllerTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "feat: HubLinkController 추가"
```

---

### Task 9: 통합 테스트 + 운영 설정 마무리 + PR

**Files:**
- Create: `server/api/src/test/java/com/junyoung/dashboard/DashboardApplicationTests.java`
- Modify: `server/api/src/main/resources/application.properties` (Postgres 데이터소스 설정 추가)

**Interfaces:**
- Consumes: Task 1~8에서 만든 hub 도메인 전체 스택

- [ ] **Step 1: application.properties에 데이터소스/JPA 설정 추가**

`server/api/src/main/resources/application.properties` 전체 내용:

```properties
spring.application.name=hubdash-api
server.port=8080

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/dashboard}
spring.datasource.username=${DB_USERNAME:junyoung}
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.hibernate.ddl-auto=validate

app.api-key=${API_KEY:dev-local-key}
```

- [ ] **Step 2: 실패하는 통합 테스트 작성**

```java
package com.junyoung.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void createsAndFetchesHubCategoryEndToEnd() throws Exception {
        HubCategoryRequest request = new HubCategoryRequest("CI/CD", "빌드 파이프라인 문서");

        mockMvc.perform(post("/api/hub/categories")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/hub/categories")
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("CI/CD"));
    }
}
```

- [ ] **Step 3: 테스트 실행 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.DashboardApplicationTests"`
Expected: PASS — 실패하면 스택 트레이스로 어느 Task 설정이 빠졌는지 확인 (대개 `application-test.properties` 누락이나 API Key 헤더 누락)

- [ ] **Step 4: 전체 빌드 확인**

Run: `./gradlew build` (레포 루트에서)
Expected: `BUILD SUCCESSFUL`, 모든 테스트 PASS

- [ ] **Step 5: 커밋**

```bash
git add api/src
git commit -m "test: hub 도메인 엔드투엔드 통합 테스트 추가, 운영 데이터소스 설정 완료"
```

- [ ] **Step 6: 이슈 #4 체크박스 갱신 + PR**

```bash
gh issue view 4 --json body -q .body
# "hub 도메인 CRUD" 체크박스를 [x]로 갱신
git push -u origin feature/domain-hub
gh pr create --base main --head feature/domain-hub \
  --title "hub 도메인 CRUD 구현" \
  --body "이슈 #4의 hub 도메인 부분 완료. HubCategory/HubLink CRUD, Flyway 마이그레이션 포함."
```

---

## 이 계획에 포함되지 않은 것

- `study`, `life`, `health`, `pknu`, `schedule`, `memo`, `user`, `reminder` 도메인 — 각자 `feature/domain-*` 브랜치의 별도 계획으로 진행
- 기존 `server/` 디렉토리 삭제 — 모든 도메인이 끝난 뒤 진행
