package com.xiaokang.equipment.dto;


/** 设备台账EquipmentEditRequest。 */
public class EquipmentEditRequest
{
    private String equipmentName;

    private String equipmentType;

    private String model;

    private String location;

    private String remark;

    private Integer version;

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

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
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
