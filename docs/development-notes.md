# 本地启动问题记录

## 2026年10月14日 Java时区参数

现象：Maven构建成功，执行Java启动命令时提示找不到主类 `.timezone=Asia.Shanghai`。

原因：PowerShell解析未加引号的 `-Duser.timezone=Asia/Shanghai` 时拆分了参数。使用Java版本检查可以复现，不需要启动完整项目。

修正命令：

```powershell
java '-Duser.timezone=Asia/Shanghai' -jar '.\ruoyi-admin\target\ruoyi-admin.jar'
```

验证：未加引号的 `java -Duser.timezone=Asia/Shanghai -version` 返回主类错误；加引号后执行 `-XshowSettings:properties -version` 返回 `user.timezone = Asia/Shanghai`。此修正不需要重新打包。

## 2026年10月14日 Redis端口不一致

现象：后端启动时连接 `localhost/127.0.0.1:6379` 被拒绝，进程退出。

原因：本机Redis服务运行，但实际监听 `127.0.0.1:6399`；两份本地Redis配置也指定6399。服务运行状态不能证明默认6379端口可访问。

修正：后端 `application.yml` 的Redis主机使用 `${EQ_REDIS_HOST:127.0.0.1}`，端口使用 `${EQ_REDIS_PORT:6399}`，密码使用 `${EQ_REDIS_PASSWORD:}`。保留原本地Redis服务设置。

验证：向实际6399端口发送PING，收到PONG；执行 `mvn package` 返回BUILD SUCCESS，新JAR配置已更新。用户重新启动后，日志显示数据库查询成功、Started RuoYiApplication及若依启动成功。后续用户已确认前端登录成功。

## 2026年10月14日 恢复直接数据库配置

用户选择本地开发维持若依原版形式。已验证保存的数据库密码可以登录 `equipment_dev` 并访问项目库，随后将用户名和密码直接填写到本地 `application-druid.yml`。主库不再依赖密码环境变量。

真实连接配置加入Git忽略规则；共享示例 `application-druid.example.yml` 不含真实密码。README移除日常启动前输入密码和保存变量的步骤。实际密码不记录到本文。
