package com.ruoyi.equipment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class EquipmentReportCount
{
    @JsonSerialize(using = ToStringSerializer.class)
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private long count;

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
