# 业务数据库：P2至P5

项目库为 `equipment_maintenance`，MySQL8、InnoDB、utf8mb4。完整字段定义见README第8节和实际DDL `sql/001_business_schema.sql`。

## 执行顺序

1. 在项目库导入固定若依基座的 `equipment-backend/sql/ry_20260417.sql` 与 `quartz.sql`。
2. 执行 `sql/001_business_schema.sql`。
3. 执行 `sql/002_business_dict_menu.sql`。
4. 执行 `sql/003_work_order_record.sql` 与 `sql/004_work_order_dict_menu.sql`。
5. 执行 `sql/005_work_order_process_review_menu.sql`，新增处理、提交及复核按钮权限。
6. 执行 `sql/006_statistics_export_menu.sql`，新增统计页面及工单导出权限。
7. 重启后端并重新登录，加载字典、菜单及权限。

本机已完成P2至P5迁移，原版31表加三张业务表共34表。业务脚本无DROP/TRUNCATE，菜单脚本重复执行不重复新增，也不重置已有配置。`CREATE TABLE IF NOT EXISTS`仅处理缺失表，后续结构升级新增编号脚本，不能以重跑001代替迁移。

## 已建立结构

| 表 | 当前用途 | 约束与索引 |
| --- | --- | --- |
| `eq_equipment` | 设备台账基本信息、使用状态和版本 | BIGINT自增主键；设备编号唯一；类型+状态组合索引 |
| `eq_work_order` | 报修、分配、取消、维护处理及复核的工单主表，支持设备历史及未结束检查 | 工单编号唯一；设备、报修人、处理人外键；设备时间、报修人时间、处理人状态时间、状态时间索引 |

`eq_work_order_record`为P3新增动作记录表，含工单、操作者外键及时间线索引，记录字段见下文。

设备编号32、名称100、类型32、型号100、位置200、状态32、说明2000字符。版本初始0；修改由条件更新递增。必填、唯一和引用约束由数据库执行，格式与状态规则在Service校验。主外键与原版`sys_user.user_id`使用一致的有符号BIGINT，外键DDL已在本机执行成功。没有级联删除。

工单字段与README8.4一致，P3已有创建、分配、取消写入接口。003迁移新增eq_work_order_record，字段与README8.5一致，工单和操作者有非级联外键，索引(work_order_id,create_time,record_id)。P3追加CREATE、ASSIGN、CANCEL；P4追加PROCESS_NOTE、SUBMIT_REVIEW、REVIEW_REJECT、REVIEW_APPROVE。每轮结果写记录表的cause_analysis、measures、result_description，不覆盖历史。

## 字典和权限数据

002脚本初始化 `eq_equipment_type`（MOTOR、TEST_BENCH、ACQUISITION、OTHER）、`eq_equipment_status`、`eq_work_order_status`。

设备维护管理目录、设备台账页面和六个功能权限按钮共8项。三个角色 `eq_manager`、`eq_reporter`、`eq_maintainer`按README矩阵建立角色菜单关系；不分配现有用户角色。004脚本已补充故障类别和优先级字典、故障工单页面及查询/创建/分配/取消权限；不分配已有用户角色。

字典状态文字仅作显示，设备状态代码是固定业务契约。停用类型不允许新选择，已有设备保持类型可编辑其他信息；显示或导出找不到名称时回退代码，不丢失原值。

## 事务约定

编辑和状态调整锁定设备行，再校验版本并写入。状态调整采用READ_COMMITTED，等待设备行锁后查询工单能够看到最新已提交数据。报废检查未结束状态集合为WAITING、PROCESSING、PENDING_REVIEW。

P3报修接口在事务中先锁设备再查在用状态，并将工单及记录一起写入。按「设备 → 工单」顺序取锁。P3已使用实际API验证报废与报修竞争，以及分配与取消竞争；结果见验证记录。

SQL初始化不包含样例业务数据。P2阶段的三条模拟设备由接口及浏览器验收创建，该阶段模拟用户已停用；P3接口验收另外创建了明确标注的模拟设备与工单，未清理正常开发数据。后续P5/P6数据与账号状态见对应阶段记录。单连接MySQL事务测试使用连接级临时业务表克隆；正式外键及跨连接行为不以临时表测试替代。

## P4变化

005仅新增三个功能权限和相应角色授权：eq_maintainer获得process/submit，eq_manager获得review。多角色账号权限取并集，但处理动作仍要求当前handler_id匹配。脚本已重复执行两次，三个权限各一条，不改变已有用户的角色关联。无新增业务表或字段，项目库仍34表。

追加处理也更新工单version与审计时间；提交写last_submit_time，通过写closed_time，退回保留原处理人和之前的提交时间。条件更新检查ID、当前状态、版本，维护动作另外限定handler_id。结果字段仅在提交记录中保存，其他动作不填充。事务测试已覆盖四种新动作的记录写入失败回滚，以及两轮提交历史保留；跨连接冲突另用真实API验收。

## P5变化

006新增 `equipment:statistics:query` 统计页面权限与 `equipment:workorder:export` 导出按钮权限。统计分配给eq_manager，导出分配给eq_manager、eq_reporter、eq_maintainer；不会修改现有账号的业务角色。本机执行两次后两个权限各一条，无新增业务表或字段，项目库仍34表。

统计按 `eq_work_order.create_time` 使用左闭右开日期区间，设备状态查询独立于工单筛选；类别及排名显式排除CANCELLED。平均闭环时长以 `TIMESTAMPDIFF(SECOND, create_time, closed_time)/3600.0` 计算已关闭工单的平均值，保留两位小数；没有已关闭工单时为NULL。多条汇总查询在同一只读REPEATABLE_READ事务内完成，没有新增缓存表或Redis统计缓存。

工单列表与导出共用 `FilteredOrders` SQL片段，包含角色数据范围和筛选，固定按create_time、work_order_id倒序。导出不使用分页，读取LIMIT 1001探测超限；1000条允许，1001条拒绝。导出同样使用只读REPEATABLE_READ事务。

新增4项真实MySQL测试，累计14项；使用连接级临时业务表验证统计边界、排名、范围及1000/1001条导出，并检查实际事务只读及隔离级别。临时表不复制正式外键，且未专门模拟统计中途跨连接写入。使用说明及字段契约见 [统计与工单导出](statistics-and-export.md)。

## P6空库复现与演示数据

本机在独立MySQL3307实例的 `equipment_maintenance_demo` 从完全空库导入两份固定框架SQL及001至006，共34表。初始化脚本会在任何导入前检查非空库并拒绝；已有库升级不能运行该脚本。独立Redis6400与正常开发Redis6399分开。

`scripts/seed_demo.py` 经正常API创建10台模拟设备、20张工单、63条动作和5个单角色账号，实际用户ID从接口解析。数据库中5条业务外键均存在；向不存在设备创建工单、向不存在工单创建动作，真实MySQL均返回1452且记录未留下。业务001至006重跑后设备、工单、动作、用户数量不变，没有同父级、类型、路径及权限的重复业务菜单；历史入口与工单页复用相同读取权限属于原设计，不以全库权限字符串唯一作为约束。

框架SQL未重跑。演示脚本再次运行会因已有凭据或非空业务库拒绝，不修改现有用户密码。操作步骤见 [本地运行说明](local-setup.md)，实际结果见 [验证记录](verification.md)。
