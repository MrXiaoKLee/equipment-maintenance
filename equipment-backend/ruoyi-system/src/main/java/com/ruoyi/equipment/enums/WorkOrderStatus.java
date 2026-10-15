package com.ruoyi.equipment.enums;

import com.ruoyi.common.exception.ServiceException;

public enum WorkOrderStatus
{
    WAITING, PROCESSING, PENDING_REVIEW, CLOSED, CANCELLED;

    public static WorkOrderStatus parse(String value)
    {
        try
        {
            return valueOf(value);
        }
        catch (IllegalArgumentException | NullPointerException ex)
        {
            throw new ServiceException("无效的工单状态", 400);
        }
    }
}
