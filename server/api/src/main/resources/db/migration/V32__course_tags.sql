-- 과목 태그 (쉼표 구분, 메모 태그와 같은 방식) (이슈 #175)
ALTER TABLE pknu_course ADD COLUMN tags VARCHAR(300);
