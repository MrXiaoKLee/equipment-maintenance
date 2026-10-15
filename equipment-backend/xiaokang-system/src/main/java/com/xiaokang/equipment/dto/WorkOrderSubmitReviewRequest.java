package com.xiaokang.equipment.dto;

/** 每轮处理结果独立保存，不接收状态、处理人或流程时间。 */
public class WorkOrderSubmitReviewRequest
{
    private Integer version;
    private String causeAnalysis;
    private String measures;
    private String resultDescription;

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getCauseAnalysis() { return causeAnalysis; }
    public void setCauseAnalysis(String causeAnalysis) { this.causeAnalysis = causeAnalysis; }
    public String getMeasures() { return measures; }
    public void setMeasures(String measures) { this.measures = measures; }
    public String getResultDescription() { return resultDescription; }
    public void setResultDescription(String resultDescription) { this.resultDescription = resultDescription; }
}
