package com.xiaokang.equipment.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/** 设备台账EquipmentWorkOrder。 */
public class EquipmentWorkOrder
{
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workOrderId;

    private String workOrderNo;

    private String title;

    private String symptom;

    private String faultCategory;

    private String priority;

    private String status;

    private String reporterName;

    private String handlerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date occurredTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getReporterName()
    {
        return reporterName;
    }

    public void setReporterName(String reporterName)
    {
        this.reporterName = reporterName;
    }

    public String getHandlerName()
    {
        return handlerName;
    }

    public void setHandlerName(String handlerName)
    {
        this.handlerName = handlerName;
    }

    public Date getOccurredTime()
    {
        return occurredTime;
    }

    public void setOccurredTime(Date occurredTime)
    {
        this.occurredTime = occurredTime;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

}
