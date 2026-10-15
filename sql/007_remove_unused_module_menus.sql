-- 代码精简后停用已移除的基座功能入口；保留原有菜单、授权及历史数据。
START TRANSACTION;

SET @cleanup_notice=(SELECT menu_id FROM sys_menu WHERE component='system/notice/index' LIMIT 1);
SET @cleanup_job=(SELECT menu_id FROM sys_menu WHERE component='monitor/job/index' LIMIT 1);
SET @cleanup_gen=(SELECT menu_id FROM sys_menu WHERE component='tool/gen/index' LIMIT 1);

UPDATE sys_menu SET status='1',visible='1',update_by='admin',update_time=NOW()
WHERE component IN ('system/notice/index','monitor/job/index','tool/gen/index','tool/build/index','tool/swagger/index')
   OR parent_id IN (@cleanup_notice,@cleanup_job,@cleanup_gen);

-- 官方工具目录下的三个页面均已移除；不匹配其他自定义目录。
UPDATE sys_menu SET status='1',visible='1',update_by='admin',update_time=NOW()
WHERE parent_id=0 AND path='tool' AND menu_type='M';

COMMIT;
