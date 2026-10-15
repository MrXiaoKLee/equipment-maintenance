package com.xiaokang.equipment.vo;

import java.util.Date;
import java.util.Map;
import com.xiaokang.common.annotation.Excel;
import com.xiaokang.equipment.domain.WorkOrder;

/** 仅导出列表字段和流程时间，不包含内部版本、账号ID或完整处理时间线。 */
public class WorkOrderExportRow
{
    @Excel(name = "工单编号", sort = 1, width = 24)
    private String workOrderNo;
    @Excel(name = "标题", sort = 2, width = 36, wrapText = true)
    private String title;
    @Excel(name = "设备编号", sort = 3, width = 24)
    private String equipmentCode;
    @Excel(name = "设备名称", sort = 4, width = 24)
    private String equipmentName;
    @Excel(name = "故障类别", sort = 5, width = 24)
    private String faultCategory;
    @Excel(name = "优先级", sort = 6, width = 24)
    private String priority;
    @Excel(name = "故障现象", sort = 7, width = 36, wrapText = true)
    private String symptom;
    @Excel(name = "报修人", sort = 8, width = 24)
    private String reporterName;
    @Excel(name = "维护人员", sort = 9, width = 24)
    private String handlerName;
    @Excel(name = "工单状态", sort = 10, width = 24)
    private String status;
    @Excel(name = "故障发生时间", sort = 11, dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private Date occurredTime;
    @Excel(name = "报修提交时间", sort = 12, dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private Date createTime;
    @Excel(name = "分配时间", sort = 13, dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private Date assignedTime;
    @Excel(name = "最近提交复核时间", sort = 14, dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private Date lastSubmitTime;
    @Excel(name = "关闭时间", sort = 15, dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private Date closedTime;
    @Excel(name = "取消时间", sort = 16, dateFormat = "yyyy-MM-dd HH:mm:ss", width = 22)
    private Date cancelledTime;

    public static WorkOrderExportRow from(WorkOrder order, Map<String, String> categories, Map<String, String> priorities, Map<String, String> statuses)
    {
        WorkOrderExportRow row = new WorkOrderExportRow();
        row.workOrderNo = order.getWorkOrderNo();
        row.title = order.getTitle();
        row.equipmentCode = order.getEquipmentCode();
        row.equipmentName = order.getEquipmentName();
        row.faultCategory = categories.getOrDefault(order.getFaultCategory(), order.getFaultCategory());
        row.priority = priorities.getOrDefault(order.getPriority(), order.getPriority());
        row.symptom = order.getSymptom();
        row.reporterName = order.getReporterName();
        row.handlerName = order.getHandlerName();
        row.status = statuses.getOrDefault(order.getStatus(), order.getStatus());
        row.occurredTime = order.getOccurredTime();
        row.createTime = order.getCreateTime();
        row.assignedTime = order.getAssignedTime();
        row.lastSubmitTime = order.getLastSubmitTime();
        row.closedTime = order.getClosedTime();
        row.cancelledTime = order.getCancelledTime();
        return row;
    }

    public String getWorkOrderNo() { return workOrderNo; }
    public String getTitle() { return title; }
    public String getEquipmentCode() { return equipmentCode; }
    public String getEquipmentName() { return equipmentName; }
    public String getFaultCategory() { return faultCategory; }
    public String getPriority() { return priority; }
    public String getSymptom() { return symptom; }
    public String getReporterName() { return reporterName; }
    public String getHandlerName() { return handlerName; }
    public String getStatus() { return status; }
    public Date getOccurredTime() { return occurredTime; }
    public Date getCreateTime() { return createTime; }
    public Date getAssignedTime() { return assignedTime; }
    public Date getLastSubmitTime() { return lastSubmitTime; }
    public Date getClosedTime() { return closedTime; }
    public Date getCancelledTime() { return cancelledTime; }
}
