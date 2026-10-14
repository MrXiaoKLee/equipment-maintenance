package com.ruoyi.equipment.dto;


/** 设备台账EquipmentStatusRequest。 */
public class EquipmentStatusRequest
{
    private String equipmentStatus;

    private Integer version;

    public String getEquipmentStatus()
    {
        return equipmentStatus;
    }

    public void setEquipmentStatus(String equipmentStatus)
    {
        this.equipmentStatus = equipmentStatus;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

}
