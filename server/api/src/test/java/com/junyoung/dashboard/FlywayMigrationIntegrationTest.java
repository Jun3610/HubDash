package com.junyoung.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

// 다른 테스트와 달리 test 프로파일(H2, flyway 비활성)을 쓰지 않고 실제 Postgres 컨테이너에 V1~V12를 적용한다 — Docker 필요.
// 리스너 자동 시작을 꺼서 존재하지 않는 Kafka 브로커에 연결 시도하지 않게 한다 (이 테스트의 관심사가 아님).
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private DataSource dataSource;

    @Test
    void allMigrationsApplySuccessfully() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank")) {
            int count = 0;
            while (resultSet.next()) {
                count++;
                assertThat(resultSet.getBoolean("success"))
                        .as("migration V%s applied successfully", resultSet.getString("version"))
                        .isTrue();
            }
            assertThat(count).isEqualTo(12);
        }
    }

    @Test
    void allDomainTablesExist() throws Exception {
        String[] expectedTables = {
                "hub_category", "hub_link",
                "study_topic", "study_progress",
                "life_habit", "life_habit_log", "life_reading_log",
                "health_log", "health_meal_record", "health_workout_log", "health_raw_log",
                "pknu_semester", "pknu_course", "pknu_assignment",
                "schedule_event", "schedule_raw_event",
                "memo", "memo_raw",
                "user_profile", "user_setting",
                "reminder"
        };

        try (Connection connection = dataSource.getConnection()) {
            for (String table : expectedTables) {
                try (ResultSet rs = connection.getMetaData().getTables(null, "public", table, null)) {
                    assertThat(rs.next()).as("table '%s' exists", table).isTrue();
                }
            }
        }
    }
}
