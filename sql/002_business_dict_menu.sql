-- 设备台账字典、菜单和角色；重复执行不删除已有数据。

START TRANSACTION;

INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time) SELECT '设备类型','eq_equipment_type','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='eq_equipment_type');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 1,'电机','MOTOR','eq_equipment_type','primary','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_equipment_type' AND dict_value='MOTOR');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 2,'试验台','TEST_BENCH','eq_equipment_type','success','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_equipment_type' AND dict_value='TEST_BENCH');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 3,'采集设备','ACQUISITION','eq_equipment_type','warning','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_equipment_type' AND dict_value='ACQUISITION');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 4,'其他','OTHER','eq_equipment_type','info','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_equipment_type' AND dict_value='OTHER');

INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time) SELECT '设备使用状态','eq_equipment_status','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='eq_equipment_status');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 1,'在用','IN_SERVICE','eq_equipment_status','success','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_equipment_status' AND dict_value='IN_SERVICE');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 2,'停用','OUT_OF_SERVICE','eq_equipment_status','warning','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_equipment_status' AND dict_value='OUT_OF_SERVICE');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 3,'报废','RETIRED','eq_equipment_status','danger','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_equipment_status' AND dict_value='RETIRED');

INSERT INTO sys_dict_type (dict_name,dict_type,status,create_by,create_time) SELECT '工单状态','eq_work_order_status','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='eq_work_order_status');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 1,'待受理','WAITING','eq_work_order_status','info','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_work_order_status' AND dict_value='WAITING');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 2,'处理中','PROCESSING','eq_work_order_status','primary','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_work_order_status' AND dict_value='PROCESSING');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 3,'待复核','PENDING_REVIEW','eq_work_order_status','warning','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_work_order_status' AND dict_value='PENDING_REVIEW');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 4,'已关闭','CLOSED','eq_work_order_status','success','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_work_order_status' AND dict_value='CLOSED');

INSERT INTO sys_dict_data (dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time) SELECT 5,'已取消','CANCELLED','eq_work_order_status','info','N','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='eq_work_order_status' AND dict_value='CANCELLED');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '设备维护管理',0,0,'equipment','EquipmentMaintenance',1,0,'M','0','0','','build','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=0 AND path='equipment');

SET @eq_parent = (SELECT menu_id FROM sys_menu WHERE parent_id=0 AND path='equipment' LIMIT 1);

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '设备台账',@eq_parent,1,'asset','equipment/asset/index','EquipmentAsset',1,0,'C','0','0','equipment:asset:list','table','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_parent AND path='asset');

SET @eq_asset = (SELECT menu_id FROM sys_menu WHERE parent_id=@eq_parent AND path='asset' LIMIT 1);

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '设备详情',@eq_asset,1,'#',1,0,'F','0','0','equipment:asset:query','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_asset AND perms='equipment:asset:query');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '新增设备',@eq_asset,2,'#',1,0,'F','0','0','equipment:asset:add','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_asset AND perms='equipment:asset:add');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '编辑设备',@eq_asset,3,'#',1,0,'F','0','0','equipment:asset:edit','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_asset AND perms='equipment:asset:edit');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '设备状态',@eq_asset,4,'#',1,0,'F','0','0','equipment:asset:status','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_asset AND perms='equipment:asset:status');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '导出设备',@eq_asset,5,'#',1,0,'F','0','0','equipment:asset:export','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_asset AND perms='equipment:asset:export');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) SELECT '设备工单历史',@eq_asset,6,'#',1,0,'F','0','0','equipment:workorder:list','#','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id=@eq_asset AND perms='equipment:workorder:list');

INSERT INTO sys_role (role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time) SELECT '设备管理人员','eq_manager',10,'1',1,1,'0','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='eq_manager' AND del_flag='0');

SET @eq_role=(SELECT role_id FROM sys_role WHERE role_key='eq_manager' AND del_flag='0' LIMIT 1);

INSERT INTO sys_role_menu (role_id,menu_id) SELECT @eq_role,m.menu_id FROM sys_menu m WHERE (m.menu_id=@eq_parent OR m.parent_id=@eq_parent OR m.parent_id=@eq_asset) AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=@eq_role AND rm.menu_id=m.menu_id);

INSERT INTO sys_role (role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time) SELECT '报修人员','eq_reporter',11,'1',1,1,'0','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='eq_reporter' AND del_flag='0');

SET @eq_role=(SELECT role_id FROM sys_role WHERE role_key='eq_reporter' AND del_flag='0' LIMIT 1);

INSERT INTO sys_role_menu (role_id,menu_id) SELECT @eq_role,m.menu_id FROM sys_menu m WHERE (m.menu_id IN (@eq_parent,@eq_asset) OR (m.parent_id=@eq_asset AND m.perms IN ('equipment:asset:query','equipment:workorder:list'))) AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=@eq_role AND rm.menu_id=m.menu_id);

INSERT INTO sys_role (role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time) SELECT '维护人员','eq_maintainer',12,'1',1,1,'0','0','admin',NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='eq_maintainer' AND del_flag='0');

SET @eq_role=(SELECT role_id FROM sys_role WHERE role_key='eq_maintainer' AND del_flag='0' LIMIT 1);

INSERT INTO sys_role_menu (role_id,menu_id) SELECT @eq_role,m.menu_id FROM sys_menu m WHERE (m.menu_id IN (@eq_parent,@eq_asset) OR (m.parent_id=@eq_asset AND m.perms IN ('equipment:asset:query','equipment:workorder:list'))) AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=@eq_role AND rm.menu_id=m.menu_id);

COMMIT;
