package com.ruoyi.equipment.enums;

import com.ruoyi.common.exception.ServiceException;

public enum WorkOrderPriority
{
    LOW, NORMAL, HIGH;

    public static WorkOrderPriority parse(String value)
    {
        try
        {
            return valueOf(value);
        }
        catch (IllegalArgumentException | NullPointerException ex)
        {
            throw new ServiceException("无效的优先级", 400);
        }
    }
}
