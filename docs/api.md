# 已实现接口：设备台账

当前文档覆盖P2。前端开发代理将 `/dev-api` 转发至本机8080，并去除该前缀。请求携带若依 `Authorization: Bearer <token>`；不在示例中保存真实令牌。

| 方法与路径 | 权限 | 参数与响应 |
| --- | --- | --- |
| GET `/equipment/asset/list` | `equipment:asset:list` | 编号、名称模糊筛选；类型、状态精确筛选；`pageNum`默认1、`pageSize`默认10且上限100；响应`code,msg,rows,total` |
| GET `/equipment/asset/options` | `equipment:asset:query` | `keyword`匹配编号或名称；强制只查在用设备，分页上限100 |
| GET `/equipment/asset/{id}` | `equipment:asset:query` | `data`为设备详情，含`version`、`openWorkOrderCount` |
| POST `/equipment/asset` | `equipment:asset:add` | JSON新增设备；`data`为创建后的设备 |
| PUT `/equipment/asset/{id}` | `equipment:asset:edit` | JSON编辑允许字段和版本号；返回最新设备 |
| PUT `/equipment/asset/{id}/status` | `equipment:asset:status` | JSON状态和版本号；返回最新设备 |
| GET `/equipment/asset/{id}/work-orders` | `equipment:workorder:list` | 分页历史，服务端按当前业务角色限定工单范围 |
| POST `/equipment/asset/export` | `equipment:asset:export` | 表单编码的相同筛选参数；成功返回XLSX，超过1000条返回业务错误 |

列表和导出支持 `equipmentCode`、`equipmentName`、`equipmentType`、`equipmentStatus`，亦支持供设备选择使用的`keyword`。固定按设备ID倒序，客户端排序参数不改变SQL排序。

新增请求示例：

```json
{
  "equipmentCode": "EQ-0001",
  "equipmentName": "模拟电机",
  "equipmentType": "MOTOR",
  "model": "DEMO-M1",
  "location": "模拟实验室A",
  "remark": "演示数据"
}
```

编辑请求示例：

```json
{
  "equipmentName": "模拟电机",
  "equipmentType": "MOTOR",
  "model": "DEMO-M2",
  "location": "模拟实验室B",
  "remark": "位置调整",
  "version": 0
}
```

状态请求示例：

```json
{ "equipmentStatus": "OUT_OF_SERVICE", "version": 1 }
```

编号、初始状态、作者、创建时间和ID不能通过编辑请求更改。`equipmentId`、`workOrderId`在JSON中为字符串；版本号为整数。时间格式`yyyy-MM-dd HH:mm:ss`，按Asia/Shanghai解释。

业务码沿用README：200成功、400校验或状态规则错误、403权限拒绝、404设备不存在、409版本冲突。HTTP状态与JSON业务码分开，客户端须读取业务码；导出失败也返回JSON业务错误。未登录使用原版401处理。

工单历史按设备ID固定过滤，管理人员查全部，报修人查本人报修，维护人查本人处理；多角色范围取并集。返回工单编号、标题、现象、类别、优先级、状态、报修与维护人员显示名、发生与创建时间。当前不提供工单处理和记录时间线接口，后续P3/P4补充。
