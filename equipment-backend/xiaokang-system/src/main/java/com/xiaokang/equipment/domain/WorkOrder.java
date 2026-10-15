package com.xiaokang.equipment.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
/** 工单阶段的数据契约。 */
public class WorkOrder
{
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workOrderId;

    private String workOrderNo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long equipmentId;

    private String equipmentCode;

    private String equipmentName;

    private String title;

    private String symptom;

    private String faultCategory;

    private String priority;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date occurredTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long reporterId;

    private String reporterName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long handlerId;

    private String handlerName;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date assignedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date lastSubmitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date closedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date cancelledTime;

    private Integer version;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updateTime;

    public Long getWorkOrderId()
    {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId)
    {
        this.workOrderId = workOrderId;
    }

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

    public String getEquipmentCode()
    {
        return equipmentCode;
    }

    public void setEquipmentCode(String equipmentCode)
    {
        this.equipmentCode = equipmentCode;
    }

    public String getEquipmentName()
    {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName)
    {
        this.equipmentName = equipmentName;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSymptom()
    {
        return symptom;
    }

    public void setSymptom(String symptom)
    {
        this.symptom = symptom;
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

    public Date getOccurredTime()
    {
        return occurredTime;
    }

    public void setOccurredTime(Date occurredTime)
    {
        this.occurredTime = occurredTime;
    }

    public Long getReporterId()
    {
        return reporterId;
    }

    public void setReporterId(Long reporterId)
    {
        this.reporterId = reporterId;
    }

    public String getReporterName()
    {
        return reporterName;
    }

    public void setReporterName(String reporterName)
    {
        this.reporterName = reporterName;
    }

    public Long getHandlerId()
    {
        return handlerId;
    }

    public void setHandlerId(Long handlerId)
    {
        this.handlerId = handlerId;
    }

    public String getHandlerName()
    {
        return handlerName;
    }

    public void setHandlerName(String handlerName)
    {
        this.handlerName = handlerName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getAssignedTime()
    {
        return assignedTime;
    }

    public void setAssignedTime(Date assignedTime)
    {
        this.assignedTime = assignedTime;
    }

    public Date getLastSubmitTime()
    {
        return lastSubmitTime;
    }

    public void setLastSubmitTime(Date lastSubmitTime)
    {
        this.lastSubmitTime = lastSubmitTime;
    }

    public Date getClosedTime()
    {
        return closedTime;
    }

    public void setClosedTime(Date closedTime)
    {
        this.closedTime = closedTime;
    }

    public Date getCancelledTime()
    {
        return cancelledTime;
    }

    public void setCancelledTime(Date cancelledTime)
    {
        this.cancelledTime = cancelledTime;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

}
