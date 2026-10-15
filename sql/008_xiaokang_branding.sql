-- 仅替换上游初始化数据中的默认显示名称；不修改账号、密码、权限和业务记录。
-- 可重复执行；已有自定义昵称和部门名称不会被覆盖。
START TRANSACTION;

UPDATE sys_user SET nick_name = 'XiaoKang'
WHERE user_id IN (1, 2) AND nick_name = '若依';

UPDATE sys_dept SET dept_name = 'XiaoKang科技'
WHERE dept_id = 100 AND dept_name = '若依科技';

UPDATE sys_dept SET leader = 'XiaoKang'
WHERE dept_id BETWEEN 100 AND 109 AND leader = '若依';

COMMIT;
