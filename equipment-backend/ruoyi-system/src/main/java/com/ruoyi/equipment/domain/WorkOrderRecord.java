package com.ruoyi.equipment.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
/** 工单阶段的数据契约。 */
public class WorkOrderRecord
{
    @JsonSerialize(using = ToStringSerializer.class)
    private Long recordId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long workOrderId;

    private String actionType;

    private String fromStatus;

    private String toStatus;

    private String description;

    private String causeAnalysis;

    private String measures;

    private String resultDescription;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long actorId;

    private String actorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getWorkOrderId()
    {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId)
    {
        this.workOrderId = workOrderId;
    }

    public String getActionType()
    {
        return actionType;
    }

    public void setActionType(String actionType)
    {
        this.actionType = actionType;
    }

    public String getFromStatus()
    {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus)
    {
        this.fromStatus = fromStatus;
    }

    public String getToStatus()
    {
        return toStatus;
    }

    public void setToStatus(String toStatus)
    {
        this.toStatus = toStatus;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getCauseAnalysis()
    {
        return causeAnalysis;
    }

    public void setCauseAnalysis(String causeAnalysis)
    {
        this.causeAnalysis = causeAnalysis;
    }

    public String getMeasures()
    {
        return measures;
    }

    public void setMeasures(String measures)
    {
        this.measures = measures;
    }

    public String getResultDescription()
    {
        return resultDescription;
    }

    public void setResultDescription(String resultDescription)
    {
        this.resultDescription = resultDescription;
    }

    public Long getActorId()
    {
        return actorId;
    }

    public void setActorId(Long actorId)
    {
        this.actorId = actorId;
    }

    public String getActorName()
    {
        return actorName;
    }

    public void setActorName(String actorName)
    {
        this.actorName = actorName;
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
