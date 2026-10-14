# 验证记录

日期：2026-10-14。实现提交：`be94f47`（后续修正见 Git 提交记录）。环境：Windows PowerShell，Java17、Maven3.9.16、Node24.13.0，MySQL8.0.37；项目库 `equipment_maintenance`，Redis6399，后端8080，前端5173。

## P2设备台账

| 项目 | 方法及实际结果 |
| --- | --- |
| SQL结构 | 本机执行001脚本，设备及工单表创建成功，项目库33表；外键DDL被MySQL接受 |
| SQL重复初始化 | 002菜单脚本执行两次，业务菜单8项、业务角色3项，无重复；保留原版系统数据 |
| 后端构建 | `mvn package '-DskipTests=false' '-Dmaven.test.skip=false'` 完整多模块打包成功 |
| 单元测试 | JUnit5 + Mockito共20项，Failures=0、Errors=0、Skipped=0；覆盖编号、字段、角色、停用账号、版本、报废、历史范围、分页清理、导出上限与字典回退，不连接或清理开发数据库 |
| 前端构建 | `npm.cmd run build:prod` 成功；没有强制升级若依依赖 |
| 运行 | 最新后端启动成功，MyBatis映射加载正常，实际HTTP接口响应正常 |
| EQ-01 / T-01 | API新增模拟设备成功，编号首尾空格删除、转大写、ID为字符串、初始在用；重复编号400，原记录未覆盖 |
| EQ-02 | API编辑名称成功，伪造编号和状态字段不改变设备编号、状态，返回版本递增 |
| EQ-03 | 按编号筛选，total=1、rows=1；两条真实数据按每页1条查询第2页，total=2、rows=1；停用设备不出现在在用选择接口 |
| EQ-04 | API停用、恢复在用成功；报修账号直接调用状态接口403；实际API报废200、再次恢复400；未结束工单报废拒绝通过Service单元测试 |
| EQ-05 | 实际API无工单返回空历史；报修账号可读取设备历史入口；角色范围参数并集及分页顺序通过单元测试。非空多角色历史与完整工单详情留待P3/P4验收 |
| EQ-06 / T-16 | 按编号导出真实XLSX，解压核对编号、电机与在用中文标签存在；报修账号导出403；超1000条拒绝通过单元测试，1001条真实数据库导出尚未执行 |
| T-09部分 | 实际API旧版本编辑409；多线程真实数据库竞争尚未执行 |
| 权限 | 实际新建模拟报修账号，读取200、新增403、状态403、导出403；停用账号后使用原令牌读取403；模拟账号验收后均停用 |
| 错误 | 未登录读取401、不存在设备404；缺失字典及无效字段通过单元测试，返回可理解的业务错误 |
| 浏览器 | 已授权本次本地登录并完成验证码。空表单拦截必填项；新增EQ-UI-0001成功并规范化为大写；编辑编号禁用，名称位置修改后列表详情一致；状态调整显示0条未结束工单，停用及恢复成功；报废设备没有状态按钮；历史显示范围内暂无工单；编号筛选1条，重置恢复3条；浏览器下载XLSX成功且内容一致。控制台无error/warn |

测试没有清空项目库或Redis。API及浏览器验收使用三条明确标注的模拟设备，保留供本地演示；模拟报修账号已停用，测试令牌已登出。自动化数据库集成测试仍须使用独立测试库，本次实际API检查不等同于完整集成测试套件。

可复用命令：

```powershell
Set-Location 'D:\VSCode\workspace\equipment-maintenance\equipment-backend'
mvn -pl ruoyi-system -am test '-DskipTests=false' '-Dmaven.test.skip=false'
mvn package '-DskipTests=false' '-Dmaven.test.skip=false'

Set-Location 'D:\VSCode\workspace\equipment-maintenance\equipment-frontend'
npm.cmd run build:prod
```

构建前须先停止占用当前JAR的后端。单元报告位于 `equipment-backend/ruoyi-system/target/surefire-reports/`。本机API结果和导出样本位于忽略提交的 `equipment-backend/ruoyi-admin/target/equipment-api-results.json` 与 `equipment-demo.xlsx`；结果文件不包含令牌或密码。

## 浏览器验收证据

验收设备EQ-UI-0001，模拟采集设备，页面新增后编辑名称和位置，停用后恢复在用。桌面表格各列、状态和操作按钮显示正常；临时验收窗口尺寸已恢复。

![设备台账浏览器验收](evidence/equipment-ledger-20261014.jpg)

浏览器下载文件：本机 Downloads/equipment_1791990569167.xlsx。实际解析工作表共2行（表头+1条筛选数据），包含EQ-UI-0001、采集设备、模拟实验室C、在用。下载样本未提交Git。

## 尚未验证的第一版内容

T-02至T-08的完整工单流程、T-10至T-15的工单事务与竞争、T-17维护账号分配、T-18全新空库演示及统计均依赖后续模块，不能以台账测试替代。本次仅验证T-01、T-09的过期版本部分、T-13的Service规则、T-16的台账部分。
