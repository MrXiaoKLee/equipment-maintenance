# 业务数据库：P2与P3

项目库为 `equipment_maintenance`，MySQL8、InnoDB、utf8mb4。完整字段定义见README第8节和实际DDL `sql/001_business_schema.sql`。

## 执行顺序

1. 在项目库导入固定若依基座的 `equipment-backend/sql/ry_20260417.sql` 与 `quartz.sql`。
2. 执行 `sql/001_business_schema.sql`。
3. 执行 `sql/002_business_dict_menu.sql`。
4. 执行 `sql/003_work_order_record.sql` 与 `sql/004_work_order_dict_menu.sql`。
5. 重启后端并重新登录，加载字典、菜单及权限。

本机已完成P2及P3，原版31表加三张业务表共34表。业务脚本无DROP/TRUNCATE，菜单脚本重复执行不重复新增，也不重置已有配置。`CREATE TABLE IF NOT EXISTS`仅处理缺失表，后续结构升级新增编号脚本，不能以重跑001代替迁移。

## 已建立结构

| 表 | 当前用途 | 约束与索引 |
| --- | --- | --- |
| `eq_equipment` | 设备台账基本信息、使用状态和版本 | BIGINT自增主键；设备编号唯一；类型+状态组合索引 |
| `eq_work_order` | 报修、分配、取消的工单主表，支持设备历史及未结束检查 | 工单编号唯一；设备、报修人、处理人外键；设备时间、报修人时间、处理人状态时间、状态时间索引 |

`eq_work_order_record`为P3新增动作记录表，含工单、操作者外键及时间线索引，记录字段见下文。

设备编号32、名称100、类型32、型号100、位置200、状态32、说明2000字符。版本初始0；修改由条件更新递增。必填、唯一和引用约束由数据库执行，格式与状态规则在Service校验。主外键与原版`sys_user.user_id`使用一致的有符号BIGINT，外键DDL已在本机执行成功。没有级联删除。

工单字段与README8.4一致，P3已有创建、分配、取消写入接口。003迁移新增eq_work_order_record，字段与README8.5一致，工单和操作者有非级联外键，索引(work_order_id,create_time,record_id)。P3仅追加CREATE、ASSIGN、CANCEL，P4再追加维护与复核动作。

## 字典和权限数据

002脚本初始化 `eq_equipment_type`（MOTOR、TEST_BENCH、ACQUISITION、OTHER）、`eq_equipment_status`、`eq_work_order_status`。

设备维护管理目录、设备台账页面和六个功能权限按钮共8项。三个角色 `eq_manager`、`eq_reporter`、`eq_maintainer`按README矩阵建立角色菜单关系；不分配现有用户角色。004脚本已补充故障类别和优先级字典、故障工单页面及查询/创建/分配/取消权限；不分配已有用户角色。

字典状态文字仅作显示，设备状态代码是固定业务契约。停用类型不允许新选择，已有设备保持类型可编辑其他信息；显示或导出找不到名称时回退代码，不丢失原值。

## 事务约定

编辑和状态调整锁定设备行，再校验版本并写入。状态调整采用READ_COMMITTED，等待设备行锁后查询工单能够看到最新已提交数据。报废检查未结束状态集合为WAITING、PROCESSING、PENDING_REVIEW。

P3报修接口在事务中先锁设备再查在用状态，并将工单及记录一起写入。按「设备 → 工单」顺序取锁。P3已使用实际API验证报废与报修竞争，以及分配与取消竞争；结果见验证记录。

SQL初始化不包含样例业务数据。开发库目前的三条模拟设备由接口及浏览器验收创建，模拟用户已停用；P3接口验收另外创建了明确标注的模拟设备与工单，未清理正常开发数据。单连接MySQL事务测试使用连接级临时业务表克隆；正式外键及跨连接行为不以临时表测试替代。
