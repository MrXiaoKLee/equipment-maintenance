# 若依源码导入基线

本文后续的「本次调整」「下一阶段」记录导入时的状态，保留作历史来源证据。当前开发状态以根README为准；v0.1.0历史交付见 `docs/release-v0.1.0.md`。当前代码已将包名和模块统一为XiaoKang，本文保留上游原始名称，见 [命名调整](xiaokang-rename.md)。

本项目已导入 RuoYi 3.9.2 后端和 Vue 3 前端，分别位于 `equipment-backend` 和 `equipment-frontend`。以下提交是本项目后续改造的固定起点，分支继续更新不会自动改变本地源码。

## 后端

| 项目 | 固定内容 |
| --- | --- |
| 官方项目 | RuoYi-Vue |
| 官方GitHub | https://github.com/yangzongzhuan/RuoYi-Vue |
| 实际导入仓库 | https://gitee.com/y_project/RuoYi-Vue.git |
| 分支 | `springboot3` |
| 提交 | `a51a838b71b446ea27256900efe7ed2faa2a02fd` |
| 上游提交日期 | 2026-08-26 |
| 框架版本 | 3.9.2 |
| Java | 17 |
| Spring Boot | 3.5.16 |
| MyBatis Spring Boot Starter | 3.0.5 |
| 本地目录 | `equipment-backend` |
| 框架SQL | `sql/ry_20260417.sql`、`sql/quartz.sql`，均位于后端目录 |

使用 Spring Boot 3 分支，未使用后端默认分支。主入口为 `ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java`，业务框架包含六个根 Maven 模块。

## 前端

| 项目 | 固定内容 |
| --- | --- |
| 官方项目 | RuoYi-Vue3 |
| 官方GitHub | https://github.com/yangzongzhuan/RuoYi-Vue3 |
| 实际导入仓库 | https://gitcode.com/yangzongzhuan/RuoYi-Vue3.git |
| 分支 | `master` |
| 提交 | `838965c5a18d2c61b73ec30c6e288057aaa08b63` |
| 上游提交日期 | 2026-08-26 |
| 框架版本 | 3.9.2 |
| Vue | 3.5.26 |
| Vite | 6.4.1 |
| Element Plus | 2.13.1 |
| 脚本语言 | JavaScript |
| 本地目录 | `equipment-frontend` |

前后端配置文件均声明3.9.2。登录、接口、权限和运行兼容性仍以本地启动验证为准。

## 导入方式

- 导入日期：2026-10-14，Asia/Shanghai。
- GitHub直连失败后，改用若依官方项目的Gitee后端和GitCode前端仓库。
- 在独立临时目录克隆指定分支，通过 `git archive` 导出该提交的跟踪文件，再解包至本项目。
- 未导入上游 `.git` 目录；本项目采用一个根Git仓库管理前后端。
- 各自保留上游 README、LICENSE、SQL、源码和配置模板。
- 初始化Git，不在本次导入中创建提交。后续配置和验证完成后再保存可运行基线。

## 本次本地调整

1. 新增根 `.gitignore`，忽略构建产物、运行日志、真实本地配置与临时导入标记。
2. 前端 `.gitignore` 移除忽略 `package-lock.json` 的规则，使本项目采用npm时可以提交锁文件；保留对 `yarn.lock` 的忽略。
3. 新增本基线记录和第三方来源说明，更新根README导入状态。

以上为导入时的调整。按用户要求，主库连接现维持原版直接配置：本地 `application-druid.yml` 填写数据库 `equipment_maintenance`、账号 `equipment_dev` 和已验证密码，不再依赖 `EQ_DB_USER`、`EQ_DB_PASSWORD`。实际配置被Git忽略，共享示例为 `application-druid.example.yml`，不含真实密码。Redis仍默认 `127.0.0.1:6399`、无密码。31张框架表已初始化，后端此前启动成功；本次直接密码登录MySQL验证通过。项目标题、前端端口、Token密钥和业务模块尚未改造，前端依赖已安装并生成package-lock.json，用户已确认原版登录成功；前端生产构建及其他管理页面待验证。

## 当前终端环境

| 工具 | 已检查的本地版本 |
| --- | --- |
| Java和javac | 17.0.20.1 |
| Maven | 3.9.16，使用Java 17 |
| Node.js | 24.13.0 |
| npm | 11.6.2 |
| MySQL客户端 | 8.0.37 |
| Git | 2.49.0.windows.1 |

MySQL与Redis的Windows服务已观察到运行。Redis在6399端口监听且PING通过；后端已成功启动，日志显示数据库查询成功。用户已确认前端启动并成功登录；其他管理页面待验证。当前数据库密码直接保存在本地配置，日常启动不需要输入密码或设置环境变量。

## 下一阶段

前后端原版登录链路已跑通。后续进入README的P2设备台账阶段：

1. 将设备字段、状态和验收条件整理为实际建表及接口设计。
2. 创建设备表、字典与业务菜单，生成基础代码骨架。
3. 实现新增、编辑、分页筛选、状态调整及服务端权限校验。
4. 验证编号唯一、字段校验和角色限制，记录真实结果。

前端生产构建、其他管理页面及Token密钥调整另按README清单验证，不因登录成功标为全部已完成。
