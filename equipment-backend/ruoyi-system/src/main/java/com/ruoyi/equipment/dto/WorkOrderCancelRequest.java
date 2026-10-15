package com.ruoyi.equipment.dto;


/** 工单阶段的数据契约。 */
public class WorkOrderCancelRequest
{
    private Integer version;

    private String description;

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
