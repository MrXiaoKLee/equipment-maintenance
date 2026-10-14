package com.ruoyi.equipment.dto;


/** 设备台账EquipmentQuery。 */
public class EquipmentQuery
{
    private String equipmentCode;

    private String equipmentName;

    private String equipmentType;

    private String equipmentStatus;

    private String keyword;

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

    public String getEquipmentType()
    {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType)
    {
        this.equipmentType = equipmentType;
    }

    public String getEquipmentStatus()
    {
        return equipmentStatus;
    }

    public void setEquipmentStatus(String equipmentStatus)
    {
        this.equipmentStatus = equipmentStatus;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

}
