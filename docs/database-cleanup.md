# 数据库精简

2026-10-15，在当前代码已移除公告、Quartz任务、代码生成与表单工具的基础上，将项目数据库从34张表精简为17张表。迁移文件为 `sql/010_remove_unused_framework_data.sql`。

## 当前表结构

| 用途 | 保留的表 |
| --- | --- |
| 设备及工单 | `eq_equipment`、`eq_work_order`、`eq_work_order_record` |
| 用户、角色及菜单 | `sys_user`、`sys_role`、`sys_menu`、`sys_user_role`、`sys_role_menu` |
| 部门与岗位 | `sys_dept`、`sys_post`、`sys_role_dept`、`sys_user_post` |
| 字典与配置 | `sys_dict_type`、`sys_dict_data`、`sys_config` |
| 日志 | `sys_oper_log`、`sys_logininfor` |

移除的17张表：

- `gen_table`、`gen_table_column`。
- `sys_job`、`sys_job_log`。
- `sys_notice`、`sys_notice_read`。
- Quartz的11张表：`QRTZ_BLOB_TRIGGERS`、`QRTZ_CRON_TRIGGERS`、`QRTZ_SIMPLE_TRIGGERS`、`QRTZ_SIMPROP_TRIGGERS`、`QRTZ_FIRED_TRIGGERS`、`QRTZ_PAUSED_TRIGGER_GRPS`、`QRTZ_SCHEDULER_STATE`、`QRTZ_LOCKS`、`QRTZ_CALENDARS`、`QRTZ_TRIGGERS`、`QRTZ_JOB_DETAILS`。本机MySQL以小写表名保存。

## 菜单、授权与字典

010先删除已停用的公告、任务、生成器及工具按钮，再删除没有剩余子菜单的旧页面和工具目录，同时删除对应的 `sys_role_menu` 行。已停用的原版官网入口也按原ID和URL匹配清理。

有自定义子菜单的目录/页面保留，避免产生孤立菜单。其他菜单、角色、账号、用户角色关系和业务授权不修改；不会按“所有停用菜单”批量删除。

移除的字典类型仅为 `sys_job_status`、`sys_job_group`、`sys_notice_type`、`sys_notice_status`，相应字典项同时移除。设备、工单、用户和日志使用的字典继续保留。

## 空库初始化

`scripts/initialize_database.py` 仍只接受已经创建的全空本地数据库。顺序为固定基座 `ry_20260417.sql`、业务001至010，不再执行 `quartz.sql`。基座暂时创建的旧公告、任务和生成器表由010删除，最终严格核对17张表的名称集合。

上游原始SQL作为固定来源保留；不通过改写历史DDL或重导框架SQL升级已有库。

## 已有库升级与恢复

1. 确认运行的是当前精简代码，已移除旧公告、任务和生成器功能。
2. 用Navicat备份或mysqldump完整备份当前项目库，保存表结构、数据及菜单授权，并在独立空库验证可恢复。
3. 按顺序执行尚未执行的编号迁移，再执行010。无需重新导入框架SQL。
4. 删除Redis中以下四个缓存键，或重启项目后端重新初始化字典缓存；刷新页面，重新登录加载当前权限。

```text
xiaokang:sys_dict:sys_job_status
xiaokang:sys_dict:sys_job_group
xiaokang:sys_dict:sys_notice_type
xiaokang:sys_dict:sys_notice_status
```

010的元数据删除使用事务；DROP TABLE是MySQL DDL，不能通过ROLLBACK恢复。删表按子表到父表排列，保持外键检查开启。脚本可重复执行，但不因此省略首次执行前的备份。

需要恢复时，先将完整备份导入另一个新建空库并核对数据，再将后端连接切换到恢复库。不要把完整备份直接覆盖到已有新记录的库中。

本机完整备份为 `.local/database-cleanup/20261015-212129/before-cleanup.sql`，已实际恢复到独立MySQL3307中的临时库验证；备份和测试日志被Git忽略。备份包含账号等数据，仅供本地恢复使用。

## 实际验证

- 完整备份恢复后，34张表逐行与源库快照一致。
- 独立恢复库及当前项目库各执行010两次，第二次不再改变表或数据。
- 当前项目库为17张表；保留表中未涉及清理的行逐行不变，菜单、角色菜单关联及字典仅移除目标行。
- 移除23项菜单、23条对应角色菜单关联、4组字典及8条字典项；无孤立菜单或角色菜单关联。
- 9台设备、15张工单、44条处理记录及21个用户保留；5个业务外键保留。
- 全新空库初始化为17张表；再次初始化被拒绝且数据不变；自定义子菜单与其目录、授权保留检查通过。
- 后端85项测试全部通过，包括14项真实MySQL临时表测试，无跳过。
- 当前运行中的8080后端通过17项检查，覆盖验证码登录、账号权限、菜单、用户、角色、部门、岗位、配置、日志、设备、工单、统计与业务字典。
- 仅清除四个旧字典缓存键，未清空Redis；当前前后端服务保持运行。

早期v0.1.0演示库、Git标签和发布包保留当时状态，不作为当前运行数据库结构的说明。
