package com.ruoyi.equipment.dto;


/** 工单阶段的数据契约。 */
public class WorkOrderCreateRequest
{
    private Long equipmentId;

    private String title;

    private String symptom;

    private String faultCategory;

    private String priority;

    private String occurredTime;

    private Boolean confirmExisting;

    public Long getEquipmentId()
    {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId)
    {
        this.equipmentId = equipmentId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSymptom()
    {
        return symptom;
    }

    public void setSymptom(String symptom)
    {
        this.symptom = symptom;
    }

    public String getFaultCategory()
    {
        return faultCategory;
    }

    public void setFaultCategory(String faultCategory)
    {
        this.faultCategory = faultCategory;
    }

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public String getOccurredTime()
    {
        return occurredTime;
    }

    public void setOccurredTime(String occurredTime)
    {
        this.occurredTime = occurredTime;
    }

    public Boolean getConfirmExisting()
    {
        return confirmExisting;
    }

    public void setConfirmExisting(Boolean confirmExisting)
    {
        this.confirmExisting = confirmExisting;
    }

}
