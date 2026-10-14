package com.ruoyi.equipment.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/** 设备台账Equipment。 */
public class Equipment extends BaseEntity
{
    @JsonSerialize(using = ToStringSerializer.class)
    private Long equipmentId;

    @Excel(name = "设备编号")
    private String equipmentCode;

    @Excel(name = "设备名称")
    private String equipmentName;

    private String equipmentType;

    @JsonIgnore
    @Excel(name = "设备类型")
    private String equipmentTypeLabel;

    public String getEquipmentTypeLabel()
    {
        return equipmentTypeLabel;
    }

    public void setEquipmentTypeLabel(String equipmentTypeLabel)
    {
        this.equipmentTypeLabel = equipmentTypeLabel;
    }

    @Excel(name = "型号")
    private String model;

    @Excel(name = "位置")
    private String location;

    @Excel(name = "使用状态", readConverterExp = "IN_SERVICE=在用,OUT_OF_SERVICE=停用,RETIRED=报废")
    private String equipmentStatus;

    private Integer version;

    /** 详情查询计算，不持久化。 */
    private Integer openWorkOrderCount;

    public Integer getOpenWorkOrderCount()
    {
        return openWorkOrderCount;
    }

    public void setOpenWorkOrderCount(Integer openWorkOrderCount)
    {
        this.openWorkOrderCount = openWorkOrderCount;
    }

    public Long getEquipmentId()
    {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId)
    {
        this.equipmentId = equipmentId;
    }

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

    @Override
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    public Date getCreateTime()
    {
        return super.getCreateTime();
    }

    @Override
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    public Date getUpdateTime()
    {
        return super.getUpdateTime();
    }
}
