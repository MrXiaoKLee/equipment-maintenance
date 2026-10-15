# 已实现接口：设备台账与故障工单

当前文档覆盖P2与P3。前端开发代理将 `/dev-api` 转发至本机8080，并去除该前缀。请求携带若依 `Authorization: Bearer <token>`；不在示例中保存真实令牌。

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

设备历史按设备ID固定过滤，并复用以下工单详情与记录接口；读取范围见下节。

## P3工单接口

| 方法与路径 | 权限 | 参数与响应 |
| --- | --- | --- |
| GET `/equipment/workorder/list` | `equipment:workorder:list` | 范围内分页列表，响应code/msg/rows/total |
| GET `/equipment/workorder/{id}` | `equipment:workorder:query` | 工单详情，包括原始现象、设备、人员及流程时间 |
| GET `/equipment/workorder/{id}/records` | `equipment:workorder:query` | data为时间线记录数组，按createTime、recordId升序 |
| GET `/equipment/workorder/maintainers` | `equipment:workorder:assign` | keyword、pageNum、pageSize；有效维护人员分页候选，仅userId/userName/nickName |
| POST `/equipment/workorder` | `equipment:workorder:add` | JSON创建报修，返回创建后工单 |
| POST `/equipment/workorder/{id}/assign` | `equipment:workorder:assign` | JSON分配，返回最新工单和version |
| POST `/equipment/workorder/{id}/cancel` | `equipment:workorder:cancel` | JSON取消，返回最新工单和version |

列表参数：workOrderNo、equipmentId、status、faultCategory、priority、beginDate、endDate；管理人员另可用reporterId/handlerId精确筛选，或reporterName/handlerName按姓名或账号模糊筛选。普通用户的人员筛选被忽略，后端总是先叠加本人的角色范围。日期基于报修create_time，结束日期包含整日。openOnly=true只查待受理、处理中、待复核，用于同设备已有工单提示。默认每页10、最大100、create_time及ID倒序。

创建示例：

```json
{
  "equipmentId": "3",
  "title": "模拟采集设备读数异常",
  "symptom": "模拟通道读数波动，仅用于演示。",
  "faultCategory": "ACQUISITION",
  "priority": "NORMAL",
  "occurredTime": "2026-10-14 09:00:00",
  "confirmExisting": false
}
```

发生时间必须符合yyyy-MM-dd HH:mm:ss有效日历值，按Asia/Shanghai解释，不能晚于服务端当前时间。仅eq_reporter或超级管理员可创建。报修人、状态、编号和createTime由服务端确定，请求中的同名伪造字段不绑定。可见同设备未结束工单大于0且confirmExisting不是true时返回409，要求查看并确认继续；其他账号不可见工单不会触发该提示。该确认不承诺严格创建幂等。

分配示例：

```json
{ "handlerId": "105", "version": 0, "description": "模拟分配，请排查连接与通道状态。" }
```

仅管理人员分配待受理工单。目标必须启用、未删除，且有启用且未删除的eq_maintainer角色；失败400。分配说明可选，最多1800字。

取消示例：

```json
{ "version": 0, "reason": "模拟误报，取消后保留原始记录。" }
```

本人报修或管理人员可取消待受理工单，reason必填且最多2000字，兼容description作为旧字段别名。处理中、已取消等状态不能取消。版本缺失400，版本过期409；动作权限不足403；范围外详情、记录、对象动作按不存在404。

读取范围：管理人员全部，报修人员reporter_id=本人，维护人员handler_id=本人，多角色取并集。设备历史采用同一规则。数据库ID均为JSON字符串，版本为整数。记录保留操作者显示名快照；无记录修改/删除、原始报修修改、处理中转派、维护处理与复核接口。请求解析错误返回400及固定提示，持久化错误返回500及固定提示，不把SQL或堆栈返回前端。
