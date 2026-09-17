package com.junyoung.dashboard.domain.pknu.repository;

import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class SemesterRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        Semester saved = semesterRepository.save(
                new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30)));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void deletingSemesterCascadesToCourses() {
        Semester semester = entityManager.persistAndFlush(
                new Semester("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30)));
        entityManager.persistAndFlush(new Course(semester, "자료구조", "김교수", 3));
        entityManager.clear();

        Long semesterId = semester.getId();
        Semester reloaded = semesterRepository.findById(semesterId).orElseThrow();
        semesterRepository.delete(reloaded);
        entityManager.flush();

        // 다른 @SpringBootTest(Kafka 통합 테스트 등)가 같은 공유 DB에 커밋한 무관한 Course 행이
        // 있을 수 있으므로, 전역 count() 대신 이 semesterId로 좁혀서 검증한다.
        assertThat(courseRepository.findBySemesterId(semesterId, Pageable.unpaged()).getContent()).isEmpty();
    }
}
