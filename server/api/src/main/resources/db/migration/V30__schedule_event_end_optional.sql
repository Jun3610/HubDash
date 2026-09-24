-- 일정은 시작만 적고 끝나는 시각은 필요할 때만 (이슈 #156)
ALTER TABLE schedule_event ALTER COLUMN end_at DROP NOT NULL;
