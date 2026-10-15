package com.ruoyi.equipment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** 工单阶段的数据契约。 */
public class WorkOrderCancelRequest
{
    private Integer version;

    @JsonAlias("description")
    private String reason;

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

}
