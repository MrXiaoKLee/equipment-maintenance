# 设备台账模块（P2）

## 已实现的功能

设备维护管理 → 设备台账提供编号、名称、类型和使用状态筛选，分页、新增、编辑、详情、状态调整、设备工单历史及 Excel 导出。复用若依的登录、布局、权限菜单、字典和操作日志。

编号必填，去除首尾空格后转为大写，只允许1至32位字母、数字、`-`、`_`。数据库唯一约束防止并发新增重复编号。创建后编号不可修改，编辑接口只接收名称、类型、型号、位置、说明与版本号。

名称最多100字、型号100字、位置200字、说明2000字；名称、类型、位置不能为空。新设备默认在用。新类型须存在且启用，已有设备的停用类型可保持原值；页面及导出在找不到字典名称时保留类型代码。

列表每页默认10条，上限100条，按设备ID倒序。设备ID以字符串传输。导出遵守当前筛选，不受当前页限制，超过1000条要求缩小范围，不静默截断。

## 初始化与访问

本机项目库已执行以下两份脚本：

1. `sql/001_business_schema.sql`：创建设备表和工单预留表，不删除已有表或数据。
2. `sql/002_business_dict_menu.sql`：设备类型、设备状态、工单状态字典，设备菜单、按钮权限、三个业务角色及角色菜单关系。

在其他开发环境中，先按 README 导入原版 SQL，再在 `equipment_maintenance` 库依次执行这两份业务脚本。菜单脚本可重复执行，不重新覆盖已经存在的字典、角色或菜单设置；DDL 的 `IF NOT EXISTS` 不负责升级已有表结构。

本机表数由31变为33。原版系统模块仍保留。

后端修改后，先关闭正在运行的后端再打包，避免 Windows 占用 JAR：

```powershell
Set-Location 'D:\VSCode\workspace\equipment-maintenance\equipment-backend'
mvn package '-DskipTests=false' '-Dmaven.test.skip=false'
java '-Duser.timezone=Asia/Shanghai' -jar '.\xiaokang-admin\target\xiaokang-admin.jar'
```

前端开发服务仍使用：

```powershell
Set-Location 'D:\VSCode\workspace\equipment-maintenance\equipment-frontend'
npm.cmd run dev -- --host 127.0.0.1 --port 5173
```

浏览器访问 `http://127.0.0.1:5173`。刷新页面，必要时退出后重新登录，让动态菜单重新加载；在「设备维护管理 → 设备台账」进入新页面。数据库密码仍保存在忽略提交的本地配置中，不需要每次启动输入。

## 账号与权限

| 角色 | 读取台账与详情 | 新增、编辑、状态、导出 | 工单历史 |
| --- | --- | --- | --- |
| `eq_manager` 设备管理人员 | 可以 | 可以 | 项目全量 |
| `eq_reporter` 报修人员 | 可以 | 不可以 | 自己报修的工单 |
| `eq_maintainer` 维护人员 | 可以 | 不可以 | 分配给自己的工单 |
| 若依超级管理员（用户ID=1） | 可以 | 可以 | 项目全量 |

新建普通账号后，在若依「系统管理 → 用户管理」分配对应角色。初始化脚本创建角色及权限，不自动给现有普通用户授权。多业务角色用户的历史范围取并集。

按钮权限由若依检查，Service 额外限制业务角色，并从数据库核对当前账号和角色状态。停用账号、删除账号、停用或撤销业务角色后，不能依靠尚未过期的登录缓存继续操作设备接口。若依用户查询补充了角色删除标记映射，避免把启用角色误判为无效。

菜单与按钮授权显示仍使用若依登录缓存；管理员修改授权后，用户退出重登以刷新显示。其他原版系统接口的会话策略未在本阶段改造。

## 使用状态及并发

- 在用和停用可互相切换，停用不要求先结束工单；调整框提示当前未结束工单数量。
- 待受理、处理中、待复核属于未结束工单，存在任意一条时不能报废。
- 报废为终态，保留设备和历史，无恢复及物理删除接口。
- 编辑与状态调整提交读取时的 `version`。服务端锁定设备并校验版本，写入后递增；过期请求返回业务码409。
- 状态调整使用 `READ_COMMITTED` 事务。权限查询发生在设备锁之前，等待锁后检查工单时须读取已经提交的新工单，不能读取事务早期的旧快照。
- 下一阶段创建工单也必须先锁定同一设备，再检查在用状态，随后写入工单和记录；该锁顺序是报废与报修一致性的必要条件。

HTTP状态与JSON业务码沿用若依约定：成功200、参数400、权限403、设备不存在404、版本冲突409；认证失败沿用401。前端根据业务码提示。实体的创建人、时间、初始状态由服务端生成。

## 接口与代码位置

设备接口见 README 9.2，均已接通。列表和详情分别返回 `TableDataInfo` 与 `AjaxResult`；状态修改返回最新设备及版本。

| 内容 | 项目内位置 |
| --- | --- |
| Controller | `equipment-backend/xiaokang-admin/src/main/java/com/xiaokang/web/controller/equipment/EquipmentController.java` |
| 领域对象、请求、Service、Mapper接口 | `equipment-backend/xiaokang-system/src/main/java/com/xiaokang/equipment/` |
| SQL映射 | `equipment-backend/xiaokang-system/src/main/resources/mapper/equipment/EquipmentMapper.xml` |
| Service测试 | `equipment-backend/xiaokang-system/src/test/java/com/xiaokang/equipment/service/EquipmentServiceTest.java` |
| 前端API | `equipment-frontend/src/api/equipment/asset.js` |
| 台账页面 | `equipment-frontend/src/views/equipment/asset/index.vue` |

`GET /equipment/asset/options` 仅返回在用设备，支持 `keyword` 与分页，供后续报修表单选择。`GET /equipment/asset/{id}` 增加服务端计算的 `openWorkOrderCount`，供状态调整提示。

## 工单历史与P3衔接

P3已实现报修创建、查询、分配、取消和动作记录；设备历史「查看」复用完整的P3工单详情及时间线，并重新进行对象范围校验。尚未实现维护处理、提交复核及结果时间线，详见 [故障工单模块](work-orders.md)。

## 验证与演示数据

实测结果见 `docs/verification.md`。接口及浏览器验收在开发库新增了三条明确标注模拟用途的设备（两条电机、一条采集设备），以及两个已停用的模拟报修账号；没有修改原有用户、角色授权或清空开发数据。P3另有明确标注的工单与设备验收数据，详见验证记录。

浏览器新增必填校验、保存、编辑、详情、停用与恢复、编号筛选、空历史及Excel下载均已验收。页面演示设备编号为EQ-UI-0001，当前在用。截图见 [验收页面](evidence/equipment-ledger-20261014.jpg)。
