// 서버 없이 화면을 볼 때 쓰는 목 데이터. 디자인 캔버스의 문구를 따르되 날짜는 오늘 기준으로 만든다.
import { shiftDate, todayLocalDate, weekStartOf, type LocalDate } from '../lib/date'

export type Row = Record<string, unknown> & { id: number; createdAt: string; updatedAt: string }
export type Tables = Record<string, Row[]>

let seq = 1000
export const nextId = () => ++seq

function stamp(date: LocalDate, time = '09:00:00'): string {
  return `${date}T${time}`
}

function row<T extends Record<string, unknown>>(data: T, date: LocalDate): T & Row {
  const at = stamp(date, '09:00:00.123456')
  return { id: nextId(), createdAt: at, updatedAt: at, ...data }
}

/** 결정적 의사 난수 (새로고침해도 같은 데이터) */
function rng(seed: number) {
  let s = seed
  return () => {
    s = (s * 9301 + 49297) % 233280
    return s / 233280
  }
}

export function buildSeed(today = todayLocalDate()): Tables {
  const d = (n: number) => shiftDate(today, n)
  const rnd = rng(7)

  // ---- PKNU ----
  const semStart = shiftDate(weekStartOf(today), -21)
  const semester = row({ name: '2026-2 학기', startDate: semStart, endDate: shiftDate(semStart, 111) }, semStart)
  const prevSemester = row(
    { name: '2026-1 학기', startDate: shiftDate(semStart, -200), endDate: shiftDate(semStart, -90) },
    d(-200),
  )
  const courseDefs: [string, string, number][] = [
    ['알고리즘과자료구조', '김교수', 3],
    ['데이터베이스응용', '이교수', 3],
    ['통계적데이터분석', '박교수', 3],
    ['직업과 윤리', '최교수', 1],
    ['실무전산II - C언어', '정교수', 3],
    ['프로그래밍기초2', '한교수', 3],
  ]
  const courses = courseDefs.map(([name, professor, credit]) =>
    row({ semesterId: semester.id, name, professor, credit }, semStart),
  )
  const asg = (ci: number, title: string, due: number, completed: boolean, notes: string | null = null) =>
    row({ courseId: courses[ci].id, title, dueDate: d(due), completed, notes }, d(due - 7))
  const assignments = [
    asg(0, '정렬 알고리즘 구현 과제', 1, false, '퀵정렬, 병합정렬 비교'),
    asg(1, 'ERD 설계 보고서', 3, false),
    asg(4, '포인터 실습 3', 6, false),
    asg(5, '재귀 함수 과제', 9, false),
    asg(2, '회귀분석 퀴즈', -1, true),
    asg(0, '시간복잡도 정리', -4, true),
    asg(1, 'SQL 조인 실습', -6, true),
    asg(3, '윤리 사례 발표', -9, true),
    asg(4, '포인터 실습 2', -2, true),
    asg(2, '기술통계 과제', 12, false),
  ]

  // ---- Study ----
  const topics = [
    row({ name: '리눅스 마스터 2급', description: '10월 시험 대비' }, d(-60)),
    row({ name: 'CS 기초', description: '운영체제 · 네트워크' }, d(-60)),
    row({ name: 'Spring 심화', description: '배치 · 카프카' }, d(-60)),
  ]
  const progresses: Row[] = []
  for (let i = 0; i < 60; i++) {
    const t = topics[i % 3]
    if (rnd() < 0.55) {
      progresses.push(
        row(
          {
            topicId: t.id,
            studiedAt: d(-i),
            minutes: [25, 50, 90, 40, 60][Math.floor(rnd() * 5)],
            notes: i % 4 === 0 ? '기출 풀이' : null,
          },
          d(-i),
        ),
      )
    }
  }

  // ---- Life ----
  const habitNames = ['물 2L 마시기', '유산균 챙기기', '유산소 30분', '7시간 이상 수면', '정제 탄수 줄이기']
  const habits = habitNames.map((name) => row({ name, description: null }, d(-90)))
  const habitLogs: Row[] = []
  habits.forEach((h, hi) => {
    for (let i = 0; i < 40; i++) {
      if (rnd() < 0.6 - hi * 0.05)
        habitLogs.push(row({ habitId: h.id, performedAt: d(-i), completed: true, notes: null }, d(-i)))
    }
  })
  const readingLogs = [
    row(
      {
        title: '데이터 중심 애플리케이션 설계',
        author: '마틴 클레프만',
        startedAt: d(-20),
        finishedAt: null,
        rating: null,
        notes: '6장까지',
      },
      d(-20),
    ),
    row(
      { title: '클린 코드', author: '로버트 C. 마틴', startedAt: d(-80), finishedAt: d(-40), rating: 4, notes: null },
      d(-80),
    ),
    row(
      { title: '이것이 리눅스다', author: '우재남', startedAt: d(-30), finishedAt: null, rating: null, notes: null },
      d(-30),
    ),
    row(
      {
        title: '가상 면접 사례로 배우는 대규모 시스템 설계',
        author: '알렉스 쉬',
        startedAt: d(-150),
        finishedAt: d(-110),
        rating: 5,
        notes: null,
      },
      d(-150),
    ),
  ]

  // ---- Health ----
  const mealRecords: Row[] = []
  const mealItems: Row[] = []
  const food: [string, number, number, number, number, number][] = [
    ['그릭요거트', 180, 12, 17, 5, 70],
    ['바나나', 140, 36, 1, 0, 1],
    ['닭가슴살 덮밥', 470, 58, 42, 8, 980],
    ['삶은 계란 2개', 140, 1, 12, 10, 140],
    ['프로틴 쉐이크', 150, 5, 30, 2, 200],
    ['아몬드 30g', 160, 6, 6, 13, 0],
    ['현미밥 + 연어', 520, 55, 32, 16, 420],
    ['샐러드', 180, 12, 8, 10, 380],
  ]
  const addMeal = (date: LocalDate, type: string, time: string, picks: number[]) => {
    const rec = row({ consumedAt: `${date}T${time}`, mealType: type, notes: null }, date)
    mealRecords.push(rec)
    for (const p of picks) {
      const [name, calories, carbsG, proteinG, fatG, sodiumMg] = food[p]
      mealItems.push(row({ mealRecordId: rec.id, name, calories, carbsG, proteinG, fatG, sodiumMg }, date))
    }
  }
  addMeal(today, 'BREAKFAST', '08:10:00', [0, 1])
  addMeal(today, 'LUNCH', '12:40:00', [2, 3])
  addMeal(today, 'SNACK', '16:20:00', [4, 5])
  for (let i = 1; i < 70; i++) {
    if (rnd() < 0.85) addMeal(d(-i), 'BREAKFAST', '08:00:00', [0, 1])
    if (rnd() < 0.9) addMeal(d(-i), 'LUNCH', '12:30:00', [2, 3])
    if (rnd() < 0.7) addMeal(d(-i), 'DINNER', '19:00:00', [6, 7])
  }
  const workoutLogs = [
    row(
      { performedAt: d(-1), type: '유산소', durationMinutes: 30, caloriesBurned: 280, notes: '경사 10 · 5km' },
      d(-1),
    ),
    row(
      {
        performedAt: d(-2),
        type: '웨이트 — 가슴 + 삼두',
        durationMinutes: 80,
        caloriesBurned: 420,
        notes: '유산소 30분 포함',
      },
      d(-2),
    ),
    row(
      { performedAt: d(-4), type: '웨이트 — 어깨 + 하체', durationMinutes: 70, caloriesBurned: 400, notes: null },
      d(-4),
    ),
    row(
      { performedAt: d(-6), type: '웨이트 — 등 + 이두', durationMinutes: 75, caloriesBurned: 410, notes: null },
      d(-6),
    ),
    row({ performedAt: d(-8), type: '유산소', durationMinutes: 40, caloriesBurned: 330, notes: null }, d(-8)),
  ]
  const healthLogs: Row[] = []
  for (let i = 0; i < 28; i++) {
    if (rnd() < 0.8)
      healthLogs.push(
        row(
          {
            recordedAt: `${d(-i)}T07:${String(10 + (i % 40)).padStart(2, '0')}:00`,
            weightKg: Math.round((72.4 + i * 0.03 + (rnd() - 0.5) * 0.6) * 10) / 10,
            sleepHours: Math.round((6.8 + (rnd() - 0.5) * 2) * 10) / 10,
            notes: null,
          },
          d(-i),
        ),
      )
  }

  // ---- Schedule ----
  const events = [
    row(
      {
        title: '알고리즘과자료구조',
        startAt: `${today}T10:30:00`,
        endAt: `${today}T12:00:00`,
        location: '공학관 A-203',
        description: null,
        allDay: false,
      },
      d(-10),
    ),
    row(
      {
        title: 'WAP 백엔드 회의',
        startAt: `${today}T15:00:00`,
        endAt: `${today}T16:30:00`,
        location: '동아리방',
        description: '배포 파이프라인 논의',
        allDay: false,
      },
      d(-3),
    ),
    row(
      {
        title: '웨이트 — 등 + 이두',
        startAt: `${today}T19:00:00`,
        endAt: `${today}T20:30:00`,
        location: '헬스장',
        description: null,
        allDay: false,
      },
      d(-3),
    ),
    row(
      {
        title: '데이터베이스응용',
        startAt: `${d(1)}T13:00:00`,
        endAt: `${d(1)}T14:30:00`,
        location: '공학관 B-105',
        description: null,
        allDay: false,
      },
      d(-10),
    ),
    row(
      {
        title: '리눅스 마스터 접수',
        startAt: `${d(3)}T00:00:00`,
        endAt: `${d(3)}T23:59:00`,
        location: null,
        description: '큐넷',
        allDay: true,
      },
      d(-5),
    ),
    row(
      {
        title: '통계 스터디',
        startAt: `${d(-1)}T18:00:00`,
        endAt: `${d(-1)}T20:00:00`,
        location: '도서관',
        description: null,
        allDay: false,
      },
      d(-5),
    ),
  ]

  // ---- Memo ----
  const memos = [
    row(
      {
        title: '전체적 공부 방향',
        content: '# 공부 방향\n\n- CS 기초 다지기\n- **백엔드** 프로젝트로 증명\n- 자격증은 리눅스 마스터부터',
        tags: '방향',
      },
      d(-1),
    ),
    row(
      {
        title: '공부하면서 깃허브 방향',
        content: '커밋은 작게, 이슈는 자주.\n\n1. 이슈\n2. 브랜치\n3. PR',
        tags: 'github',
      },
      d(-3),
    ),
    row({ title: 'CS 공부 방향', content: '운영체제 → 네트워크 → DB 순서로.', tags: 'CS,방향' }, d(-5)),
    row({ title: '활동 (클라우드)', content: 'AWS 스터디 정리', tags: 'cloud' }, d(-9)),
  ]

  // ---- Hub ----
  const catNames = ['CI/CD', 'Network', 'CS', 'Scripts', 'Java', 'CT', 'Docker', 'Infra', 'License']
  const hubCategories = catNames.map((name) => row({ name, description: null }, d(-120)))
  const hubLinks: Row[] = []
  const hosts = ['github.com', 'docs.docker.com', 'velog.io', 'tistory.com', 'kubernetes.io', 'baeldung.com']
  hubCategories.forEach((c, ci) => {
    const n = [6, 5, 8, 4, 7, 3, 4, 3, 2][ci]
    for (let i = 0; i < n; i++) {
      const host = hosts[(ci + i) % hosts.length]
      hubLinks.push(
        row(
          {
            categoryId: c.id,
            title: `${c.name} 자료 ${i + 1}`,
            url: `https://${host}/${String(c.name)
              .toLowerCase()
              .replace(/[^a-z]/g, '')}/${i + 1}`,
            description: i % 2 ? '정리 잘 된 글' : null,
          },
          d(-i * 3 - ci),
        ),
      )
    }
  })

  // ---- Reminder ----
  const reminders = [
    row(
      {
        title: '정렬 과제 제출',
        targetAt: `${d(1)}T09:00:00`,
        targetDomain: 'pknu',
        targetEntityId: assignments[0].id,
        sent: false,
      },
      d(-2),
    ),
    row(
      {
        title: '리눅스 마스터 접수',
        targetAt: `${d(3)}T10:00:00`,
        targetDomain: 'schedule',
        targetEntityId: events[4].id,
        sent: false,
      },
      d(-2),
    ),
    row(
      {
        title: '주간 통계 확인',
        targetAt: `${d(5)}T21:00:00`,
        targetDomain: 'health',
        targetEntityId: null,
        sent: false,
      },
      d(-2),
    ),
    row(
      { title: '영양제 주문', targetAt: `${d(-1)}T20:00:00`, targetDomain: null, targetEntityId: null, sent: false },
      d(-3),
    ),
    row(
      {
        title: 'ERD 초안 공유',
        targetAt: `${d(-2)}T18:00:00`,
        targetDomain: 'pknu',
        targetEntityId: assignments[1].id,
        sent: true,
      },
      d(-4),
    ),
  ]

  // ---- User ----
  const profile = [
    row(
      {
        displayName: '박준영',
        email: 'junp3610@gmail.com',
        bio: '부경대 시스템경영안전공학부 · 기술데이터공학 전공 · WAP 백엔드',
      },
      d(-100),
    ),
  ]
  const settings = [row({ theme: 'dark', language: 'ko', notificationEnabled: true }, d(-100))]
  // 식단 목표는 비워 둔 상태로 시작 (사용자가 정함)
  const dietGoal = [
    row(
      {
        carbsG: null,
        carbsRule: 'AT_MOST',
        fatG: null,
        fatRule: 'AT_MOST',
        proteinG: null,
        proteinRule: 'AT_LEAST',
        calories: null,
        caloriesRule: 'AT_MOST',
      },
      d(-100),
    ),
  ]

  // ---- 주간 통계 ----
  const weeks = Array.from({ length: 8 }, (_, i) => shiftDate(weekStartOf(today), -7 * (i + 1)))
  const mealStats = weeks.map((w, i) =>
    row(
      {
        weekStart: w,
        dayCount: 6 + (i % 2),
        avgCalories: [1455, 1470, 1510, 1490, 1530, 1580, 1640, 1720][i],
        avgCarbsG: 131,
        avgProteinG: 152,
        avgFatG: 44,
      },
      w,
    ),
  )
  const healthStats = weeks.map((w, i) =>
    row({ weekStart: w, logCount: 5, avgWeightKg: 72.4 + i * 0.2, avgSleepHours: 6.8 }, w),
  )
  const studyStats = topics.flatMap((t, ti) =>
    weeks.map((w, i) =>
      row(
        {
          topicId: t.id,
          weekStart: w,
          sessionCount: 3 + ((i + ti) % 3),
          totalMinutes: 120 + ((i * 37 + ti * 50) % 200),
        },
        w,
      ),
    ),
  )
  const habitStats = habits.flatMap((h, hi) =>
    weeks
      .slice(0, 4)
      .map((w, i) => row({ habitId: h.id, weekStart: w, totalCount: 7, completedCount: 3 + ((i + hi) % 4) }, w)),
  )
  const assignmentStats = courses.flatMap((c, ci) =>
    weeks.slice(0, 4).map((w, i) => {
      const total = 1 + ((ci + i) % 2)
      const done = (ci + i) % 3 === 0 ? total - 1 : total
      return row(
        { courseId: c.id, weekStart: w, totalCount: total, completedCount: done, completionRate: done / total },
        w,
      )
    }),
  )

  return {
    '/api/pknu/semesters': [semester, prevSemester],
    '/api/pknu/courses': courses,
    '/api/pknu/assignments': assignments,
    '/api/study/topics': topics,
    '/api/study/progresses': progresses,
    '/api/life/habits': habits,
    '/api/life/habit-logs': habitLogs,
    '/api/life/reading-logs': readingLogs,
    '/api/health/meal-records': mealRecords,
    '/api/health/meal-items': mealItems,
    '/api/health/workout-logs': workoutLogs,
    '/api/health/logs': healthLogs,
    '/api/schedule/events': events,
    '/api/memo/memos': memos,
    '/api/hub/categories': hubCategories,
    '/api/hub/links': hubLinks,
    '/api/reminder/reminders': reminders,
    '/api/user/profile': profile,
    '/api/user/settings': settings,
    '/api/health/diet-goal': dietGoal,
    '/api/health/analytics/meal-weekly-stats': mealStats,
    '/api/health/analytics/weekly-stats': healthStats,
    '/api/study/analytics/topic-weekly-stats': studyStats,
    '/api/life/analytics/habit-weekly-stats': habitStats,
    '/api/pknu/analytics/assignment-weekly-stats': assignmentStats,
  }
}
