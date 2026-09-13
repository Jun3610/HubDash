# Phase 2 스캐폴딩 + 공통 요소 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Gradle 멀티모듈(`common`, `api`)을 만들고 공통 요소(BaseEntity, ApiResponse, GlobalExceptionHandler, ApiKeyAuthFilter)를 완성해, 이후 도메인별 브랜치들이 공유할 기반을 `main`에 확정한다.

**Architecture:** `common` 모듈이 Entity/DTO/BaseEntity를, `api` 모듈이 Controller-Service-Repository와 `global`(config/security/exception/common) 공통 설정을 갖는다. 이 계획은 도메인 로직을 포함하지 않는다 — 도메인은 이 계획이 `main`에 머지된 뒤 각자 별도 브랜치/계획으로 진행한다.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Lombok, JUnit 5, Mockito, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-13-phase2-multi-module-domain-architecture-design.md`

**Branch:** `feature/phase2-scaffolding` (이미 생성됨, 스펙 커밋 1개 존재)

## Global Constraints

- 루트 패키지는 `com.junyoung.dashboard`.
- `common` 모듈: Entity, DTO, `BaseEntity`만 포함. `api` 모듈: Controller/Service/Repository + `global`(config/security/exception/common) 포함.
- 모든 Entity는 `BaseEntity`(id, createdAt, updatedAt, JPA Auditing) 상속.
- 모든 API 응답은 `ApiResponse<T>` 래퍼로 통일.
- 인증은 API Key 방식: 헤더 `X-API-KEY`, `ApiKeyAuthFilter`(`OncePerRequestFilter`) 하나로 처리, Spring Security 풀스택 없음.
- Java 17, Spring Boot 4.1.1 (기존 `server/build.gradle`과 동일 버전/의존성 네이밍 컨벤션 유지).
- 이 계획은 도메인 엔티티/Flyway/DB 연동을 포함하지 않는다 — 그건 `feature/domain-*` 브랜치들의 몫.

---

### Task 1: 멀티모듈 Gradle 스캐폴딩

**Files:**
- Create: `settings.gradle` (레포 루트)
- Create: `build.gradle` (레포 루트)
- Create: `common/build.gradle`
- Create: `api/build.gradle`
- Create: `api/src/main/java/com/junyoung/dashboard/DashboardApplication.java`
- Create: `api/src/main/resources/application.properties`

**Interfaces:**
- Produces: Gradle 모듈 `:common`, `:api`. `api`는 `:common`에 `implementation project(':common')`로 의존.

- [ ] **Step 1: 루트 settings.gradle 작성**

```gradle
rootProject.name = 'hubdash'
include 'common', 'api'
```

- [ ] **Step 2: 루트 build.gradle 작성**

```gradle
plugins {
    id 'org.springframework.boot' version '4.1.1' apply false
    id 'io.spring.dependency-management' version '1.1.7' apply false
}

allprojects {
    group = 'com.junyoung'
    version = '0.0.1-SNAPSHOT'

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    tasks.withType(Test) {
        useJUnitPlatform()
    }
}
```

- [ ] **Step 3: common/build.gradle 작성**

```gradle
plugins {
    id 'java-library'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.boot:spring-boot-dependencies:4.1.1"
    }
}

dependencies {
    api 'org.springframework.boot:spring-boot-starter-data-jpa'
    api 'org.springframework.boot:spring-boot-starter-validation'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.assertj:assertj-core'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

- [ ] **Step 4: api/build.gradle 작성**

```gradle
plugins {
    id 'org.springframework.boot'
    id 'java'
}

dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.flywaydb:flyway-database-postgresql'
    compileOnly 'org.projectlombok:lombok'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    runtimeOnly 'org.postgresql:postgresql'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-validation-test'
    testRuntimeOnly 'com.h2database:h2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
}
```

- [ ] **Step 5: DashboardApplication.java 작성**

```java
package com.junyoung.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}
```

- [ ] **Step 6: application.properties 작성**

```properties
spring.application.name=hubdash-api
server.port=8080
```

- [ ] **Step 7: 빌드 확인**

Run: `./gradlew build` (레포 루트에서)
Expected: `BUILD SUCCESSFUL` (테스트는 아직 없음, 컴파일 + bootJar만 성공하면 됨)

- [ ] **Step 8: 커밋**

```bash
git add settings.gradle build.gradle common/build.gradle api/build.gradle api/src
git commit -m "feat: 멀티모듈(common/api) Gradle 스캐폴딩 추가"
```

---

### Task 2: common — BaseEntity

**Files:**
- Create: `common/src/main/java/com/junyoung/dashboard/global/common/BaseEntity.java`
- Test: `common/src/test/java/com/junyoung/dashboard/global/common/BaseEntityTest.java`

**Interfaces:**
- Produces: `abstract class BaseEntity { Long getId(); LocalDateTime getCreatedAt(); LocalDateTime getUpdatedAt(); }` — 이후 모든 도메인 브랜치가 상속.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.global.common;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Entity
    static class SampleEntity extends BaseEntity {
        SampleEntity() {
        }
    }

    @Test
    void hasNullIdAndTimestampsBeforePersistence() {
        SampleEntity entity = new SampleEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.global.common.BaseEntityTest"`
Expected: FAIL (컴파일 에러 — `BaseEntity` 클래스가 없음)

- [ ] **Step 3: BaseEntity 구현**

```java
package com.junyoung.dashboard.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :common:test --tests "com.junyoung.dashboard.global.common.BaseEntityTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add common/src
git commit -m "feat: BaseEntity 추가 (JPA Auditing)"
```

---

### Task 3: api — ApiResponse<T> + JPA Auditing 설정

**Files:**
- Create: `api/src/main/java/com/junyoung/dashboard/global/common/ApiResponse.java`
- Create: `api/src/main/java/com/junyoung/dashboard/global/config/JpaAuditingConfig.java`
- Test: `api/src/test/java/com/junyoung/dashboard/global/common/ApiResponseTest.java`

**Interfaces:**
- Produces: `ApiResponse.success(T data)`, `ApiResponse.error(String errorCode, String message)`, `boolean isSuccess()`, `T getData()`, `String getErrorCode()`, `String getMessage()` — 이후 모든 도메인 Controller가 반환 타입으로 사용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.global.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successWrapsDataAndClearsError() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getMessage()).isNull();
    }

    @Test
    void errorWrapsCodeAndMessageWithNullData() {
        ApiResponse<String> response = ApiResponse.error("NOT_FOUND", "찾을 수 없음");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("찾을 수 없음");
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.global.common.ApiResponseTest"`
Expected: FAIL (컴파일 에러 — `ApiResponse` 클래스가 없음)

- [ ] **Step 3: ApiResponse 구현**

```java
package com.junyoung.dashboard.global.common;

public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String errorCode;
    private final String message;

    private ApiResponse(boolean success, T data, String errorCode, String message) {
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.global.common.ApiResponseTest"`
Expected: PASS

- [ ] **Step 5: JpaAuditingConfig 작성 (테스트 없음 — 도메인 브랜치의 @DataJpaTest에서 간접 검증)**

```java
package com.junyoung.dashboard.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

- [ ] **Step 6: 커밋**

```bash
git add api/src
git commit -m "feat: ApiResponse 래퍼와 JPA Auditing 설정 추가"
```

---

### Task 4: api — 예외 처리 (EntityNotFoundException + GlobalExceptionHandler)

**Files:**
- Create: `api/src/main/java/com/junyoung/dashboard/global/exception/EntityNotFoundException.java`
- Create: `api/src/main/java/com/junyoung/dashboard/global/exception/GlobalExceptionHandler.java`
- Test: `api/src/test/java/com/junyoung/dashboard/global/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `ApiResponse.error(String, String)` (Task 3)
- Produces: `class EntityNotFoundException extends RuntimeException`, `@RestControllerAdvice class GlobalExceptionHandler` (404는 `EntityNotFoundException`, 400은 `MethodArgumentNotValidException`을 잡아 `ApiResponse.error(...)` 반환) — 이후 모든 도메인 Service가 not-found 시 `EntityNotFoundException`을 던짐.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        public String notFound() {
            throw new EntityNotFoundException("hub category 1 not found");
        }
    }

    @Test
    void returns404WithApiResponseError() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("hub category 1 not found"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.global.exception.GlobalExceptionHandlerTest"`
Expected: FAIL (컴파일 에러 — `EntityNotFoundException`, `GlobalExceptionHandler`가 없음)

- [ ] **Step 3: EntityNotFoundException 구현**

```java
package com.junyoung.dashboard.global.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: GlobalExceptionHandler 구현**

```java
package com.junyoung.dashboard.global.exception;

import com.junyoung.dashboard.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST", message));
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.global.exception.GlobalExceptionHandlerTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add api/src
git commit -m "feat: EntityNotFoundException과 GlobalExceptionHandler 추가"
```

---

### Task 5: api — ApiKeyAuthFilter

**Files:**
- Create: `api/src/main/java/com/junyoung/dashboard/global/security/ApiKeyAuthFilter.java`
- Test: `api/src/test/java/com/junyoung/dashboard/global/security/ApiKeyAuthFilterTest.java`
- Modify: `api/src/main/resources/application.properties`

**Interfaces:**
- Produces: `class ApiKeyAuthFilter extends OncePerRequestFilter`, 생성자 `ApiKeyAuthFilter(String expectedApiKey)`, 헤더 상수 `ApiKeyAuthFilter.API_KEY_HEADER = "X-API-KEY"` — `@Component`로 등록되어 Spring Boot가 전체 요청에 자동 적용.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.junyoung.dashboard.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiKeyAuthFilterTest {

    private final ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key");

    @Test
    void allowsRequestWithCorrectApiKey() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(ApiKeyAuthFilter.API_KEY_HEADER)).thenReturn("secret-key");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void rejectsRequestWithWrongApiKey() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(ApiKeyAuthFilter.API_KEY_HEADER)).thenReturn("wrong-key");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.global.security.ApiKeyAuthFilterTest"`
Expected: FAIL (컴파일 에러 — `ApiKeyAuthFilter`가 없음)

- [ ] **Step 3: ApiKeyAuthFilter 구현**

```java
package com.junyoung.dashboard.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";

    private final String expectedApiKey;

    public ApiKeyAuthFilter(@Value("${app.api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String providedKey = request.getHeader(API_KEY_HEADER);
        if (expectedApiKey.equals(providedKey)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"errorCode\":\"UNAUTHORIZED\",\"message\":\"invalid api key\"}");
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :api:test --tests "com.junyoung.dashboard.global.security.ApiKeyAuthFilterTest"`
Expected: PASS

- [ ] **Step 5: application.properties에 API Key 설정 추가**

`api/src/main/resources/application.properties`에 아래 줄 추가:

```properties
app.api-key=${API_KEY:dev-local-key}
```

- [ ] **Step 6: 커밋**

```bash
git add api/src
git commit -m "feat: API Key 인증 필터 추가"
```

---

### Task 6: 이슈/PR 마무리

**Files:** 없음 (GitHub 작업만)

- [ ] **Step 1: 이슈 #4 체크박스 갱신**

`common, api 멀티모듈 스캐폴딩 + Flyway 세팅`(Flyway 자체는 도메인 브랜치에서 처음 쓰이므로 이 체크박스는 "멀티모듈 스캐폴딩" 부분만 완료로, Flyway는 hub 브랜치 완료 시 갱신), `global 공통 요소` 체크박스를 완료로 변경.

```bash
gh issue view 4 --json body -q .body
# 위 출력을 바탕으로 체크박스를 [x]로 바꿔 gh issue edit 4 --body "..."
```

- [ ] **Step 2: PR 생성 및 머지**

```bash
git push -u origin feature/phase2-scaffolding
gh pr create --base main --head feature/phase2-scaffolding \
  --title "Phase 2: 멀티모듈 스캐폴딩 + 공통 요소" \
  --body "이슈 #4의 스캐폴딩/global 부분 완료. 도메인은 각자 별도 브랜치(feature/domain-*)로 진행."
gh pr merge --merge
```

---

## 이 계획에 포함되지 않은 것

- 모든 도메인(hub, study, life, health, pknu, schedule, memo, user, reminder) — 각자 `feature/domain-*` 브랜치의 별도 계획으로 진행
- Flyway 마이그레이션 파일 (첫 도메인 브랜치에서 `V1__init.sql`로 시작)
- 기존 `server/` 디렉토리 삭제 — 모든 도메인이 끝난 뒤 진행
