-- 과목 대시보드: 성적(4.5 만점 등급)과 과목 메모 (이슈 #134)
ALTER TABLE pknu_course ADD COLUMN grade VARCHAR(2);
ALTER TABLE pknu_course ADD COLUMN memo TEXT;
