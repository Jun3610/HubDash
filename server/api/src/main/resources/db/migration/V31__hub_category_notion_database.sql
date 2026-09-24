-- 허브 카테고리와 노션 DB를 이어서 'Notion 불러오기'로 새 글을 가져온다 (이슈 #163)
ALTER TABLE hub_category ADD COLUMN notion_database_id VARCHAR(32);
