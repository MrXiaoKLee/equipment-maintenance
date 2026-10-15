package com.xiaokang.equipment.dto;


/** 工单阶段的数据契约。 */
public class WorkOrderAssignRequest
{
    private Long handlerId;

    private Integer version;

    private String description;

    public Long getHandlerId()
    {
        return handlerId;
    }

    public void setHandlerId(Long handlerId)
    {
        this.handlerId = handlerId;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

}
