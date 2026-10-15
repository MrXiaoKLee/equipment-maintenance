# XiaoKang 命名调整

2026-10-15 按项目命名要求，将应用自身的 RuoYi/ruoyi/若依标识统一为 XiaoKang。原有开源基座来源、许可证和原作者署名保留；改名不表示基础框架由本项目从零实现。

## 命名约定

| 用途 | 当前名称 |
| --- | --- |
| 页面、侧栏及启动标识 | `XiaoKang` |
| Java 包 | `com.xiaokang` |
| 主入口 | `com.xiaokang.XiaoKangApplication` |
| 配置类及前缀 | `XiaoKangConfig`、`xiaokang` |
| 后端模块 | `xiaokang-admin`、`xiaokang-framework`、`xiaokang-system`、`xiaokang-common` |
| 后端 JAR | `xiaokang-admin/target/xiaokang-admin.jar` |
| 前端 npm 包 | `xiaokang` |
| 前端通用工具、样式 | `src/utils/xiaokang.js`、`src/assets/styles/xiaokang.scss` |
| Redis 缓存前缀 | `xiaokang:` |

小写用于包名、目录和配置键，遵循 Java、Maven 和 npm 的命名惯例。

## 本地启动

```powershell
Set-Location 'D:\VSCode\workspace\equipment-maintenance\equipment-backend'
mvn clean package '-DskipTests=false' '-Dmaven.test.skip=false'
java '-Duser.timezone=Asia/Shanghai' -jar '.\xiaokang-admin\target\xiaokang-admin.jar'
```

另开终端启动前端：

```powershell
Set-Location 'D:\VSCode\workspace\equipment-maintenance\equipment-frontend'
npm.cmd run dev
```

默认仍使用前端5173、后端8080、MySQL3306和Redis6399。本地数据库连接文件随模块迁到 `equipment-backend/xiaokang-admin/src/main/resources/application-druid.yml`，连接信息与直接读取方式不变，该文件继续被Git忽略。

## 已有数据与缓存

- 已有库只执行 `sql/008_xiaokang_branding.sql`，不重导框架SQL。脚本只改仍等于原版默认值的用户昵称、部门名称和负责人，可重复执行，不改登录名、密码、授权或设备和工单。
- 空库初始化脚本已包含008，仍生成34张表。
- 新代码的缓存类型属于 `com.xiaokang`，因此使用独立的 `xiaokang:` 前缀，不读取旧类型的登录和字典缓存，也不清空共享Redis。升级后需重新登录。
- 本机默认上传目录改为 `D:/xiaokang/uploadPath`。本次改名已复制旧目录中已有头像，原目录仍保留。其他电脑升级时应先复制旧上传文件到新目录，或将 `xiaokang.profile` 指向原有可写目录。
- `.local` 中的旧测试副本、日志、发布包及既有 `v0.1.0` 标签记录当时的版本，不作为当前运行源码批量替换。

## 来源说明

上游LICENSE、源码作者注释和官方链接保留原名；框架初始化SQL也保留固定来源，通过008迁移调整应用显示值。当前文档中的“复用若依”等表述说明技术来源。导入基线和早期验收记录中的原始名称属于历史证据，实际运行命令以本文和根README为准。

## 本次验证

- 后端 `mvn clean package -DskipTests=false -Dmaven.test.skip=false` 成功；85项测试全部通过，含14项真实MySQL临时表测试，无跳过。
- 前端生产构建成功，工具与样式重命名后的引用可解析。
- 实际开发服务的HTML标题、重命名工具模块以及5173到8080的验证码代理响应均通过检查。
- 新JAR实际启动，验证码登录、缓存中的登录用户反序列化、菜单路由、设备列表、工单列表、统计和字典查询均通过；接口文档显示XiaoKang。
- 检查JAR入口及三个内部依赖JAR，均使用新包名和模块名。
- 本机008执行两次，显示结果幂等；设备、工单、处理记录及用户数量一致，登录名和密码未改动。
- 本地数据库连接文件迁移前后字节一致；上传头像复制时核对同名文件，未覆盖不同内容。

构建及运行验证日志在被Git忽略的 `.local/xiaokang/` 中，既有v0.1.0标签和发布包不变。
