-- P3字典、菜单与角色授权。执行于项目库，保留已有业务数据和用户授权。

START TRANSACTION;

INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time) SELECT '故障类别','eq_fault_category','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='eq_fault_category');

INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time) SELECT '优先级','eq_priority','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='eq_priority');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 1,'机械','MECHANICAL','eq_fault_category','','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_fault_category' AND dict_value='MECHANICAL');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 2,'电气','ELECTRICAL','eq_fault_category','','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_fault_category' AND dict_value='ELECTRICAL');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 3,'采集与传感','ACQUISITION','eq_fault_category','','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_fault_category' AND dict_value='ACQUISITION');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 4,'软件与控制','SOFTWARE','eq_fault_category','','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_fault_category' AND dict_value='SOFTWARE');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 5,'其他','OTHER','eq_fault_category','','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_fault_category' AND dict_value='OTHER');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 1,'低','LOW','eq_priority','info','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_priority' AND dict_value='LOW');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 2,'普通','NORMAL','eq_priority','primary','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_priority' AND dict_value='NORMAL');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 3,'高','HIGH','eq_priority','danger','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_priority' AND dict_value='HIGH');

SET @eq_parent=(SELECT menu_id FROM sys_menu WHERE parent_id=0 AND path='equipment' LIMIT 1);

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '故障工单',@eq_parent,2,'workorder','equipment/workorder/index','EquipmentWorkOrder',1,0,'C','0','0','equipment:workorder:list','form','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_parent AND path='workorder');

SET @eq_workorder=(SELECT menu_id FROM sys_menu WHERE parent_id=@eq_parent AND path='workorder' LIMIT 1);

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '工单详情与记录',@eq_workorder,1,'#',1,0,'F','0','0','equipment:workorder:query','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_workorder AND perms='equipment:workorder:query');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '提交报修',@eq_workorder,2,'#',1,0,'F','0','0','equipment:workorder:add','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_workorder AND perms='equipment:workorder:add');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '分配维护人员',@eq_workorder,3,'#',1,0,'F','0','0','equipment:workorder:assign','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_workorder AND perms='equipment:workorder:assign');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '取消报修',@eq_workorder,4,'#',1,0,'F','0','0','equipment:workorder:cancel','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_workorder AND perms='equipment:workorder:cancel');

INSERT INTO sys_role_menu (role_id,menu_id) SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON (m.menu_id IN (@eq_parent,@eq_workorder) OR (m.parent_id=@eq_workorder AND m.perms IN ('equipment:workorder:query','equipment:workorder:assign','equipment:workorder:cancel'))) WHERE r.role_key='eq_manager' AND r.del_flag='0' AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

INSERT INTO sys_role_menu (role_id,menu_id) SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON (m.menu_id IN (@eq_parent,@eq_workorder) OR (m.parent_id=@eq_workorder AND m.perms IN ('equipment:workorder:query','equipment:workorder:add','equipment:workorder:cancel'))) WHERE r.role_key='eq_reporter' AND r.del_flag='0' AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

INSERT INTO sys_role_menu (role_id,menu_id) SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON (m.menu_id IN (@eq_parent,@eq_workorder) OR (m.parent_id=@eq_workorder AND m.perms IN ('equipment:workorder:query'))) WHERE r.role_key='eq_maintainer' AND r.del_flag='0' AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

COMMIT;
