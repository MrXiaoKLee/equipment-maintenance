# 第三方源码与资源来源

本项目使用若依官方前后端作为开发基座。新增设备和工单业务功能应与框架原有功能分别说明。

| 组件 | 官方项目 | 本地许可证 | 固定提交 |
| --- | --- | --- | --- |
| Java后端 | [RuoYi-Vue](https://github.com/yangzongzhuan/RuoYi-Vue) | [后端LICENSE](equipment-backend/LICENSE) | `a51a838b71b446ea27256900efe7ed2faa2a02fd` |
| Vue 3前端 | [RuoYi-Vue3](https://github.com/yangzongzhuan/RuoYi-Vue3) | [前端LICENSE](equipment-frontend/LICENSE) | `838965c5a18d2c61b73ec30c6e288057aaa08b63` |

两份上游许可证均为MIT，原始声明为 `Copyright (c) 2018 RuoYi`，已保留在各源码目录。许可证完整条款以对应LICENSE文件为准。

实际导入使用 [官方Gitee后端仓库](https://gitee.com/y_project/RuoYi-Vue/tree/springboot3) 和 [官方GitCode前端仓库](https://gitcode.com/yangzongzhuan/RuoYi-Vue3)。版本与导入方式见 [基线记录](docs/upstream-baseline.md)。

后续如新增图片、数据集、代码或其他资源，应记录来源及对应许可。自编业务代码的发布许可在实际发布前确定；本文件不替代各依赖和资源自身的许可证。
