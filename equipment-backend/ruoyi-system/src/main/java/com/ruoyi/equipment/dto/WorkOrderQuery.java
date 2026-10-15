package com.ruoyi.equipment.dto;

import java.util.Date;
/** 工单阶段的数据契约。 */
public class WorkOrderQuery
{
    private String workOrderNo;

    private Long equipmentId;

    private String status;

    private String faultCategory;

    private String priority;

    private String beginDate;

    private String endDate;

    private Long reporterId;

    private Long handlerId;

    private Boolean openOnly;

    private Date beginTime;

    private Date endTime;

    public String getWorkOrderNo()
    {
        return workOrderNo;
    }

    public void setWorkOrderNo(String workOrderNo)
    {
        this.workOrderNo = workOrderNo;
    }

    public Long getEquipmentId()
    {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId)
    {
        this.equipmentId = equipmentId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getFaultCategory()
    {
        return faultCategory;
    }

    public void setFaultCategory(String faultCategory)
    {
        this.faultCategory = faultCategory;
    }

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public String getBeginDate()
    {
        return beginDate;
    }

    public void setBeginDate(String beginDate)
    {
        this.beginDate = beginDate;
    }

    public String getEndDate()
    {
        return endDate;
    }

    public void setEndDate(String endDate)
    {
        this.endDate = endDate;
    }

    public Long getReporterId()
    {
        return reporterId;
    }

    public void setReporterId(Long reporterId)
    {
        this.reporterId = reporterId;
    }

    public Long getHandlerId()
    {
        return handlerId;
    }

    public void setHandlerId(Long handlerId)
    {
        this.handlerId = handlerId;
    }

    public Boolean getOpenOnly()
    {
        return openOnly;
    }

    public void setOpenOnly(Boolean openOnly)
    {
        this.openOnly = openOnly;
    }

    public Date getBeginTime()
    {
        return beginTime;
    }

    public void setBeginTime(Date beginTime)
    {
        this.beginTime = beginTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    private String reporterName;
    private String handlerName;
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getHandlerName() { return handlerName; }
    public void setHandlerName(String handlerName) { this.handlerName = handlerName; }
}
