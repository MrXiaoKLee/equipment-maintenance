# XiaoKang 后端

设备故障与维护记录管理系统的 Java 后端，基于 RuoYi-Vue 3.9.2 的 Spring Boot 3 基座改造。原作者署名和 MIT 许可证保留，详见 [第三方来源](../THIRD_PARTY_NOTICES.md)。

## 模块

- `xiaokang-admin`：启动入口、接口和配置。
- `xiaokang-framework`：认证、权限及基础框架。
- `xiaokang-system`：系统管理与设备、工单、统计业务。
- `xiaokang-common`：通用模型和工具。

Java 包名为 `com.xiaokang`，入口类为 `com.xiaokang.XiaoKangApplication`。

## 本地启动

使用 Java 17、Maven、MySQL 8 和 Redis。在后端目录执行：

```powershell
mvn clean package '-DskipTests=false' '-Dmaven.test.skip=false'
java '-Duser.timezone=Asia/Shanghai' -jar '.\xiaokang-admin\target\xiaokang-admin.jar'
```

本地数据库配置为 `xiaokang-admin/src/main/resources/application-druid.yml`，共享模板为同目录的 `application-druid.example.yml`。该配置和数据库密码不提交 Git，日常启动直接读取，无须每次输入。

全新空库的上游初始账号为 `admin` / `admin123`，已有库沿用已设置的密码。数据库初始化、008 命名迁移及演示步骤见 [本地配置](../docs/local-setup.md) 与 [命名调整](../docs/xiaokang-rename.md)。

功能和验收范围以 [根 README](../README.md) 为准。
