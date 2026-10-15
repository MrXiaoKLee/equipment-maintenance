package com.ruoyi.equipment.dto;

/** 追加处理记录，只接收说明与读取时的版本。 */
public class WorkOrderProcessRequest
{
    private Integer version;
    private String description;

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
