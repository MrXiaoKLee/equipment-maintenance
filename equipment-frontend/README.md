# XiaoKang 前端

设备故障与维护记录管理系统的 Vue 3 前端，使用 Vite、Element Plus 和 Pinia。基于 RuoYi-Vue3 3.9.2 改造；保留原作者署名和 MIT 许可证，详见 [第三方来源](../THIRD_PARTY_NOTICES.md)。

## 本地开发

在当前目录执行：

```powershell
npm.cmd ci
npm.cmd run dev
```

默认地址为 `http://127.0.0.1:5173`，开发代理指向后端 `http://127.0.0.1:8080`。保持前后端终端运行，使用 Ctrl+C 停止对应进程。

```powershell
npm.cmd run build:prod
```

构建输出为 `dist`。环境文件中的 `VITE_APP_TITLE` 控制网页与登录页标题；侧栏简称为 XiaoKang，页签栏已关闭。

完整功能、配置和验收范围见 [根 README](../README.md) 和 [本地配置](../docs/local-setup.md)。
