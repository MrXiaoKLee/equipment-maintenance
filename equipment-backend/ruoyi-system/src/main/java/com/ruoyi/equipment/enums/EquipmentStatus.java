package com.ruoyi.equipment.enums;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

/** 设备使用状态，与工单状态分别管理。 */
public enum EquipmentStatus
{
    IN_SERVICE, OUT_OF_SERVICE, RETIRED;

    public static EquipmentStatus parse(String value)
    {
        try
        {
            return valueOf(value);
        }
        catch (IllegalArgumentException | NullPointerException ex)
        {
            throw new ServiceException("设备使用状态不合法", HttpStatus.BAD_REQUEST);
        }
    }
}
