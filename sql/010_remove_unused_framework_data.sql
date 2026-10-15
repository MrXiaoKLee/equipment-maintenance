-- 已有库升级前先备份。仅适用于公告、任务及生成器代码已移除的当前版本。
-- 元数据清理使用事务；后面的 DROP TABLE 为 MySQL DDL，不能通过 ROLLBACK 撤销。

CREATE TEMPORARY TABLE cleanup_menu_ids (menu_id BIGINT PRIMARY KEY);
START TRANSACTION;

-- 先移除已停用功能的按钮，不匹配其他模块的按钮。
INSERT INTO cleanup_menu_ids
SELECT menu_id FROM sys_menu
WHERE status = '1' AND menu_type = 'F'
  AND (perms LIKE 'system:notice:%' OR perms LIKE 'monitor:job:%'
       OR perms LIKE 'tool:gen:%' OR perms LIKE 'tool:build:%'
       OR perms LIKE 'tool:swagger:%');

DELETE rm FROM sys_role_menu rm JOIN cleanup_menu_ids c ON c.menu_id = rm.menu_id;
DELETE m FROM sys_menu m JOIN cleanup_menu_ids c ON c.menu_id = m.menu_id;
DELETE FROM cleanup_menu_ids;

-- 有自定义子菜单的目录/页面保留，避免产生孤立子菜单。
INSERT INTO cleanup_menu_ids
SELECT m.menu_id FROM sys_menu m
WHERE m.status = '1'
  AND m.component IN ('system/notice/index', 'monitor/job/index',
                      'tool/gen/index', 'tool/build/index', 'tool/swagger/index')
  AND NOT EXISTS (SELECT 1 FROM sys_menu child WHERE child.parent_id = m.menu_id);

DELETE rm FROM sys_role_menu rm JOIN cleanup_menu_ids c ON c.menu_id = rm.menu_id;
DELETE m FROM sys_menu m JOIN cleanup_menu_ids c ON c.menu_id = m.menu_id;
DELETE FROM cleanup_menu_ids;

INSERT INTO cleanup_menu_ids
SELECT m.menu_id FROM sys_menu m
WHERE m.status = '1' AND m.parent_id = 0 AND m.menu_type = 'M'
  AND (m.path = 'tool'
       OR (m.menu_id = 4 AND m.is_frame = 0
           AND m.path IN ('http://ruoyi.vip', 'https://ruoyi.vip',
                          'http://www.ruoyi.vip', 'https://www.ruoyi.vip')))
  AND NOT EXISTS (SELECT 1 FROM sys_menu child WHERE child.parent_id = m.menu_id);

DELETE rm FROM sys_role_menu rm JOIN cleanup_menu_ids c ON c.menu_id = rm.menu_id;
DELETE m FROM sys_menu m JOIN cleanup_menu_ids c ON c.menu_id = m.menu_id;

DELETE FROM sys_dict_data
WHERE dict_type IN ('sys_job_status', 'sys_job_group', 'sys_notice_type', 'sys_notice_status');
DELETE FROM sys_dict_type
WHERE dict_type IN ('sys_job_status', 'sys_job_group', 'sys_notice_type', 'sys_notice_status');

COMMIT;
DROP TEMPORARY TABLE cleanup_menu_ids;

-- 子表先于父表删除，保持外键检查开启。
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS gen_table_column;
DROP TABLE IF EXISTS gen_table;
DROP TABLE IF EXISTS sys_job_log;
DROP TABLE IF EXISTS sys_job;
DROP TABLE IF EXISTS sys_notice_read;
DROP TABLE IF EXISTS sys_notice;
