-- 체중·수면 기록을 날짜+시각(시:분)으로 (이슈 #137). 기존 행은 그날 00:00으로 보존한다.
ALTER TABLE health_log ALTER COLUMN recorded_at TYPE TIMESTAMP USING recorded_at::timestamp;
