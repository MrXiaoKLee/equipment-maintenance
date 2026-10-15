-- P4维护处理与复核权限。先执行001至004；不修改已有业务记录，不重分配账号角色。
START TRANSACTION;

SET @eq_parent=(SELECT menu_id FROM sys_menu WHERE parent_id=0 AND path='equipment' LIMIT 1);
SET @eq_workorder=(SELECT menu_id FROM sys_menu WHERE parent_id=@eq_parent AND path='workorder' LIMIT 1);

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '追加处理记录',@eq_workorder,5,'#',1,0,'F','0','0','equipment:workorder:process','#','admin',NOW()
WHERE @eq_workorder IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_workorder AND perms='equipment:workorder:process');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '提交处理结果',@eq_workorder,6,'#',1,0,'F','0','0','equipment:workorder:submit','#','admin',NOW()
WHERE @eq_workorder IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_workorder AND perms='equipment:workorder:submit');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '复核处理结果',@eq_workorder,7,'#',1,0,'F','0','0','equipment:workorder:review','#','admin',NOW()
WHERE @eq_workorder IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_workorder AND perms='equipment:workorder:review');

INSERT INTO sys_role_menu (role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.parent_id=@eq_workorder
AND ((r.role_key='eq_maintainer' AND m.perms IN ('equipment:workorder:process','equipment:workorder:submit'))
 OR (r.role_key='eq_manager' AND m.perms='equipment:workorder:review'))
WHERE r.del_flag='0' AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

COMMIT;
