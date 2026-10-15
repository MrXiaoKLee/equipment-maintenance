package com.ruoyi.equipment.dto;

import java.util.Date;

/** 统计只按工单提交日期与设备筛选，设备总量始终使用全部设备。 */
public class StatisticsQuery
{
    private String beginDate;
    private String endDate;
    private Long equipmentId;
    private Date beginTime;
    private Date endTime;

    public String getBeginDate() { return beginDate; }
    public void setBeginDate(String beginDate) { this.beginDate = beginDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Date getBeginTime() { return beginTime; }
    public Date getEndTime() { return endTime; }
    public void setPeriod(ReportingPeriod period) { beginTime = period.getBeginTime(); endTime = period.getEndTime(); }
}
