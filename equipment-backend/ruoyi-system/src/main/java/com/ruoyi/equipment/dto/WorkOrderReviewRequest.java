package com.ruoyi.equipment.dto;

/** decision只能为APPROVE或REJECT，说明必填。 */
public class WorkOrderReviewRequest
{
    private Integer version;
    private String decision;
    private String description;

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
