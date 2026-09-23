-- 과목/공부 주제 카드에서 해당 노션 필기 페이지로 바로 가기 위한 주소 (이슈 #112)
ALTER TABLE pknu_course ADD COLUMN notion_url VARCHAR(1000);
ALTER TABLE study_topic ADD COLUMN notion_url VARCHAR(1000);
