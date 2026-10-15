-- 停用上游默认官网入口；保留菜单记录和角色关系，不影响其他外部链接。
UPDATE sys_menu
SET status = '1', visible = '1', update_by = 'admin', update_time = NOW()
WHERE menu_id = 4 AND parent_id = 0 AND is_frame = 0
  AND path IN ('http://ruoyi.vip', 'https://ruoyi.vip',
               'http://www.ruoyi.vip', 'https://www.ruoyi.vip')
  AND (status <> '1' OR visible <> '1');
