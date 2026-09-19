# syntax=docker/dockerfile:1

# ---- 빌드 스테이지: Gradle로 실행 가능한 jar를 만든다 ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성 해석을 소스 복사보다 먼저 해서, 소스만 바뀐 재빌드에서 의존성 다운로드 레이어를 재사용한다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY common/build.gradle common/build.gradle
COPY api/build.gradle api/build.gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY common common
COPY api api
# 테스트(Testcontainers/EmbeddedKafka가 Docker를 요구)는 CI 파이프라인의 별도 단계에서 돌리므로 이미지 빌드에서는 건너뛴다.
RUN ./gradlew --no-daemon :api:bootJar -x test \
    && cp "$(ls api/build/libs/*.jar | grep -v -- '-plain.jar')" /workspace/app.jar

# ---- 런타임 스테이지: JRE만 담은 얇은 이미지 ----
FROM eclipse-temurin:17-jre
RUN groupadd --system hubdash && useradd --system --gid hubdash --no-create-home hubdash
WORKDIR /app
COPY --from=build --chown=hubdash:hubdash /workspace/app.jar app.jar

USER hubdash
EXPOSE 8080

# 컨테이너 메모리 한도(cgroup)의 일정 비율만 힙으로 쓴다. 필요하면 JAVA_OPTS로 덮어쓴다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
# 이미지는 기본으로 prod 프로파일 — API_KEY/DB_PASSWORD를 빠뜨리면 개발용 기본값으로 뜨지 않고 기동이 실패한다.
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
