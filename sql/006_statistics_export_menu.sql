-- P5统计页面与工单导出权限。先执行001至005，保留现有配置和账号角色。
START TRANSACTION;
SET @eq_parent=(SELECT menu_id FROM sys_menu WHERE parent_id=0 AND path='equipment' LIMIT 1);
SET @eq_workorder=(SELECT menu_id FROM sys_menu WHERE parent_id=@eq_parent AND path='workorder' LIMIT 1);

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '统计分析',@eq_parent,3,'statistics','equipment/statistics/index','EquipmentStatistics',1,0,'C','0','0','equipment:statistics:query','chart','admin',NOW()
WHERE @eq_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_parent AND path='statistics');
SET @eq_statistics=(SELECT menu_id FROM sys_menu WHERE parent_id=@eq_parent AND path='statistics' LIMIT 1);

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '导出工单',@eq_workorder,8,'#',1,0,'F','0','0','equipment:workorder:export','#','admin',NOW()
WHERE @eq_workorder IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_workorder AND perms='equipment:workorder:export');

INSERT INTO sys_role_menu (role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.menu_id IN (@eq_parent,@eq_statistics)
WHERE r.role_key='eq_manager' AND r.del_flag='0'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

INSERT INTO sys_role_menu (role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.parent_id=@eq_workorder AND m.perms='equipment:workorder:export'
WHERE r.role_key IN ('eq_manager','eq_reporter','eq_maintainer') AND r.del_flag='0'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);
COMMIT;
