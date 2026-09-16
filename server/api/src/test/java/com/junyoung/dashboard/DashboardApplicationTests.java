package com.junyoung.dashboard;

import tools.jackson.databind.ObjectMapper;
import com.junyoung.dashboard.domain.health.dto.HealthLogRequest;
import com.junyoung.dashboard.domain.health.dto.MealRecordRequest;
import com.junyoung.dashboard.domain.health.dto.WorkoutLogRequest;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.ReadingLogRequest;
import com.junyoung.dashboard.domain.pknu.dto.AssignmentRequest;
import com.junyoung.dashboard.domain.pknu.dto.CourseRequest;
import com.junyoung.dashboard.domain.pknu.dto.SemesterRequest;
import com.junyoung.dashboard.domain.schedule.dto.EventRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void movesStudyProgressBetweenTopicsEndToEnd() throws Exception {
        StudyTopicRequest topicARequest = new StudyTopicRequest("토익", "영어 공부");
        String topicAResponse = mockMvc.perform(post("/api/study/topics")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topicARequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long topicAId = objectMapper.readTree(topicAResponse).path("data").path("id").asLong();

        StudyTopicRequest topicBRequest = new StudyTopicRequest("알고리즘", "코딩테스트 공부");
        String topicBResponse = mockMvc.perform(post("/api/study/topics")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topicBRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long topicBId = objectMapper.readTree(topicBResponse).path("data").path("id").asLong();

        StudyProgressRequest createRequest = new StudyProgressRequest(topicAId, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이");
        String progressResponse = mockMvc.perform(post("/api/study/progresses")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.topicId").value(topicAId))
                .andReturn().getResponse().getContentAsString();
        long progressId = objectMapper.readTree(progressResponse).path("data").path("id").asLong();

        StudyProgressRequest moveRequest = new StudyProgressRequest(topicBId, LocalDate.of(2026, 9, 15), 90, "DP 복습");
        mockMvc.perform(put("/api/study/progresses/" + progressId)
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topicBId));
    }

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

    @Test
    void createsAndFetchesHealthLogEndToEnd() throws Exception {
        HealthLogRequest request = new HealthLogRequest(LocalDate.of(2026, 9, 1), 70.5, 7.5, null);

        mockMvc.perform(post("/api/health/logs")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/health/logs")
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].weightKg").value(70.5));
    }

    @Test
    void createsAndFetchesMealRecordEndToEnd() throws Exception {
        MealRecordRequest request = new MealRecordRequest(
                LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, 500, 60.0, 20.0, 15.0, 300.0, null);

        mockMvc.perform(post("/api/health/meal-records")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/health/meal-records")
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mealType").value("BREAKFAST"));
    }

    @Test
    void createsAndFetchesWorkoutLogEndToEnd() throws Exception {
        WorkoutLogRequest request = new WorkoutLogRequest(LocalDate.of(2026, 9, 1), "러닝", 30, 300, null);

        mockMvc.perform(post("/api/health/workout-logs")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/health/workout-logs")
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("러닝"));
    }

    @Test
    void createsAndFetchesSemesterEndToEnd() throws Exception {
        SemesterRequest request = new SemesterRequest("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));

        mockMvc.perform(post("/api/pknu/semesters")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/pknu/semesters")
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("2026-1학기"));
    }

    @Test
    void movesCourseBetweenSemestersEndToEnd() throws Exception {
        SemesterRequest semesterARequest = new SemesterRequest("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
        String semesterAResponse = mockMvc.perform(post("/api/pknu/semesters")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(semesterARequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long semesterAId = objectMapper.readTree(semesterAResponse).get("data").get("id").asLong();

        SemesterRequest semesterBRequest = new SemesterRequest("2026-2학기", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20));
        String semesterBResponse = mockMvc.perform(post("/api/pknu/semesters")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(semesterBRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long semesterBId = objectMapper.readTree(semesterBResponse).get("data").get("id").asLong();

        CourseRequest createRequest = new CourseRequest(semesterAId, "자료구조", "김교수", 3);
        String courseResponse = mockMvc.perform(post("/api/pknu/courses")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.semesterId").value(semesterAId))
                .andReturn().getResponse().getContentAsString();
        Long courseId = objectMapper.readTree(courseResponse).get("data").get("id").asLong();

        CourseRequest moveRequest = new CourseRequest(semesterBId, "운영체제", "이교수", 4);
        mockMvc.perform(put("/api/pknu/courses/" + courseId)
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.semesterId").value(semesterBId));
    }

    @Test
    void movesAssignmentBetweenCoursesEndToEnd() throws Exception {
        SemesterRequest semesterRequest = new SemesterRequest("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
        String semesterResponse = mockMvc.perform(post("/api/pknu/semesters")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(semesterRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long semesterId = objectMapper.readTree(semesterResponse).get("data").get("id").asLong();

        CourseRequest courseARequest = new CourseRequest(semesterId, "자료구조", "김교수", 3);
        String courseAResponse = mockMvc.perform(post("/api/pknu/courses")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseARequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long courseAId = objectMapper.readTree(courseAResponse).get("data").get("id").asLong();

        CourseRequest courseBRequest = new CourseRequest(semesterId, "운영체제", "이교수", 4);
        String courseBResponse = mockMvc.perform(post("/api/pknu/courses")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseBRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long courseBId = objectMapper.readTree(courseBResponse).get("data").get("id").asLong();

        AssignmentRequest createRequest = new AssignmentRequest(courseAId, "1주차 과제", LocalDate.of(2026, 3, 10), false, null);
        String assignmentResponse = mockMvc.perform(post("/api/pknu/assignments")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.courseId").value(courseAId))
                .andReturn().getResponse().getContentAsString();
        Long assignmentId = objectMapper.readTree(assignmentResponse).get("data").get("id").asLong();

        AssignmentRequest moveRequest = new AssignmentRequest(courseBId, "2주차 과제", LocalDate.of(2026, 3, 17), true, "이동됨");
        mockMvc.perform(put("/api/pknu/assignments/" + assignmentId)
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(courseBId));
    }

    @Test
    void createsAndFetchesEventEndToEnd() throws Exception {
        EventRequest request = new EventRequest("발표 준비", LocalDateTime.of(2026, 9, 16, 10, 0),
                LocalDateTime.of(2026, 9, 16, 11, 0), "회의실 A", null, false);

        mockMvc.perform(post("/api/schedule/events")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/schedule/events")
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("발표 준비"));
    }

    @Test
    void rejectsEventWithEndAtBeforeStartAtEndToEnd() throws Exception {
        EventRequest invalid = new EventRequest("잘못된 일정", LocalDateTime.of(2026, 9, 16, 11, 0),
                LocalDateTime.of(2026, 9, 16, 10, 0), null, null, false);

        mockMvc.perform(post("/api/schedule/events")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
