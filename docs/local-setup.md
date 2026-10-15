# 本地初始化、构建与启动

适用于新的源码副本及空演示库。当前开发环境已经运行，不要重新导入框架SQL，也不要覆盖现有 `application-druid.yml`。P6演示数据只导入独立空库，正常开发库保留原数据。

## 已验证环境

Windows PowerShell；JDK17.0.20.1、Maven3.9.16、Node24.13.0、npm11.6.2、MySQL8.0.37。可选初始化脚本使用Python3标准库，本机Python3.12.4，无须pip安装依赖。Java后端仍由Maven构建，Python只用于一次性数据库导入和演示准备。

检查本机工具：

```powershell
java -version
mvn -version
node -v
npm.cmd -v
mysql --version
python --version
```

MySQL客户端未加入PATH时，脚本可传入 `--mysql 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'`。Maven应显示使用JDK17；本机项目JDK位于 `D:\JAVA_JDK\installation_directory`。

## 1. 准备空数据库

在Navicat中以有建库授权的账号连接，创建专用空库及本地开发账号。下面的密码是占位文字，由你在本机设置，不提交到Git：

```sql
CREATE DATABASE equipment_maintenance_demo
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'equipment_demo'@'localhost'
  IDENTIFIED BY 'YOUR_LOCAL_DATABASE_PASSWORD';
GRANT ALL PRIVILEGES ON equipment_maintenance_demo.*
  TO 'equipment_demo'@'localhost';
```

新电脑也可使用README约定的 `equipment_maintenance` / `equipment_dev`。若与原开发库并行演示，使用不同的库名和Redis实例，并在独立源码副本中配置，避免两个应用共用登录及字典缓存。

在项目根目录运行一次：

```powershell
Set-Location 'D:\VSCode\workspace\equipment-maintenance'
python '.\scripts\initialize_database.py' --user equipment_demo --database equipment_maintenance_demo
```

脚本仅此次提示数据库密码，执行完临时连接配置即释放；这不改变日常直接配置的启动方式。默认连接本机3306，可传 `--port 3307`。它先确认目标库存在且完全为空，再依次导入：

1. 固定上游 `equipment-backend/sql/ry_20260417.sql`、`quartz.sql`。
2. 业务 `sql/001_business_schema.sql` 至 `006_statistics_export_menu.sql`。
3. 核对最终34张基础表，此时没有业务设备和工单。

框架SQL包含删除并重建框架表的语句，因此脚本遇到非空库会拒绝，不能当升级脚本使用。导入中断时DDL可能已经提交，脚本不自动删表或重建库；先检查报错及独立演示库，另建新的空库重试。已有项目升级只执行尚未执行的编号业务迁移。

不使用Python时，可以在确认全空的库内用Navicat「运行SQL文件」按上述顺序导入。菜单脚本可重复执行，不代表框架SQL可在已有库重跑。

## 2. 配置后端一次

仅在新的源码副本、且目标文件不存在时复制共享模板：

```powershell
$taskConfig = '.\equipment-backend\ruoyi-admin\src\main\resources\application-druid.yml'
if (Test-Path -LiteralPath $taskConfig) { throw '本地配置已存在，请直接检查，不要覆盖。' }
Copy-Item '.\equipment-backend\ruoyi-admin\src\main\resources\application-druid.example.yml' $taskConfig
```

在VS Code编辑该文件的 `spring.datasource.druid.master`：URL中的主机、端口和数据库名，以及username/password。完整格式保持模板原样。日常启动自动读取该文件，无须每个终端输入数据库密码。

`application.yml` 中核对以下配置：

| 配置 | 正常开发 | 并行P6验收时使用的值 |
| --- | --- | --- |
| `server.port` | 8080 | 8081 |
| `spring.data.redis.host/port/database` | 127.0.0.1 / 6399 / 0 | 独立Redis 127.0.0.1 / 6400 / 0 |
| `ruoyi.profile` | 可写上传目录 | 独立验收目录下uploads |
| `token.secret` | 框架默认值仍需按实际使用场景配置 | 独立副本使用本机生成值 |

同机并行应用应配置独立Redis，不能清空正常开发Redis。P6复现启动了已安装MySQL与Redis的独立进程，不新装服务、不更改原Windows服务，也不修改正常库账号授权。独立实例的端口只是验收安排，并非日常启动要求。

后端默认日志写入启动目录的 `logs`；需要时可用 `LOG_PATH` 指定路径。构建产物、本地数据库配置、日志、`.local` 和上传数据均不作为公开交付物。打包JAR包含当时的本地配置，不分享含真实密码的本机JAR。

## 3. 构建与启动后端

```powershell
Set-Location 'D:\VSCode\workspace\equipment-maintenance\equipment-backend'
mvn clean package '-DskipTests=false' '-Dmaven.test.skip=false'
java '-Duser.timezone=Asia/Shanghai' -jar '.\ruoyi-admin\target\ruoyi-admin.jar'
```

未显式配置MySQL测试连接时，71项单元执行，14项MySQL测试跳过；P6验收提供独立测试连接，85项全部执行并通过。测试变量仅测试时使用，见 `docs/work-orders.md`，不加入日常启动前置步骤。

看到 `Started RuoYiApplication` 后保持终端打开。关闭后端时在该终端按Ctrl+C；修改源码配置后应停止旧后端、重新打包再启动。项目根目录与后端目录不同，在根目录直接使用 `ruoyi-admin/target/...` 会找不到JAR。

## 4. 安装与启动前端

另开一个终端：

```powershell
Set-Location 'D:\VSCode\workspace\equipment-maintenance\equipment-frontend'
npm.cmd ci
npm.cmd run dev
```

默认只监听 `127.0.0.1:5173`，代理到 `http://127.0.0.1:8080`。以终端实际Local地址为准；端口冲突时检查原有进程。使用 `-- --strictPort` 可以让冲突直接报错。

并行环境可以在该副本创建被忽略的 `.env.development.local`：

```dotenv
VITE_APP_BACKEND_URL=http://127.0.0.1:8081
```

再使用 `npm.cmd run dev -- --port 5174 --strictPort`。此变量只用于开发代理配置，不放任何密码。仅访问后端8080/8081不会显示前端业务页面。

生产构建验证命令为 `npm.cmd run build:prod`，产物为dist。生产变量的 `/prod-api` 需要部署时配置反向代理；本版仅验收本地开发运行和构建，没有验证线上部署。

## 5. 准备演示账号及数据

确认后端指向刚初始化的独立库，并且只有原版两个账号、没有业务设备与工单，再在项目根目录执行：

```powershell
python '.\scripts\seed_demo.py' --base-url http://127.0.0.1:8080
```

并行P6环境将地址替换为8081。输入该空库的初始admin密码；固定上游初始账号说明见原版后端README。若有验证码，脚本保存图片到 `.local/demo/login-captcha.png`，打开查看后在终端输入答案。admin及5个新业务账号分别登录一次，有验证码时需依次填写；验证码设置保持原样，不关闭验证。

脚本通过正常API创建账号、台账、流转及动作记录，成功后登出全部初始化会话，演示账号保持启用。账号密码随机生成，均满足框架登录长度要求，仅保存在本地 `.local/demo/demo-accounts.json`；业务索引与统计核对结果在 `demo-manifest.json`。不在共享文档中保存这些密码，也不存储登录令牌。

脚本拒绝已有设备/工单、额外用户或已有输出凭据文件，不覆盖账号密码、不自动清库。失败可能留下部分演示数据，应检查独立库及本地凭据记录，不能向正常开发库反复重跑。详细数据与讲解步骤见 [演示指南](demo-guide.md)。

## 6. 首次检查与关闭

1. 在实际前端地址使用manager01登录：台账10台、工单20张、未结束12张、五种状态各4张。
2. reporter01和reporter02各可读10张本人工单，maintainer01和maintainer02各可读6张分配工单。
3. 设备DEMO-EQ-002历史中有一张退回重提并关闭的工单，可查看8条动作及两轮结果。
4. 统计仅管理角色可见；导出与各账号列表范围一致。所有数据均带模拟说明。
5. 正常关闭前端及后端终端使用Ctrl+C。MySQL/Redis若为共享Windows服务，不随项目关闭；若自行启动了专用实例，仅停止对应专用实例，保留数据目录以便重启。

具体实测证据见 [验证记录](verification.md) 的P6部分。首次演示不要求删除若依基座源码，业务账号菜单已按角色聚焦设备模块。
