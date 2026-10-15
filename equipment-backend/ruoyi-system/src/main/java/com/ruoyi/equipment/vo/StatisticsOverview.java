package com.ruoyi.equipment.vo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

public class StatisticsOverview
{
    private long equipmentTotal;
    private long orderTotal;
    private long openOrderCount;
    private long closedOrderCount;
    private BigDecimal averageClosedHours;
    private List<CountBucket> equipmentStatuses;
    private List<CountBucket> orderStatuses;
    private List<CountBucket> faultCategories;
    private List<EquipmentReportCount> topEquipment;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date generatedTime;

    public long getEquipmentTotal() { return equipmentTotal; }
    public void setEquipmentTotal(long value) { equipmentTotal = value; }
    public long getOrderTotal() { return orderTotal; }
    public void setOrderTotal(long value) { orderTotal = value; }
    public long getOpenOrderCount() { return openOrderCount; }
    public void setOpenOrderCount(long value) { openOrderCount = value; }
    public long getClosedOrderCount() { return closedOrderCount; }
    public void setClosedOrderCount(long value) { closedOrderCount = value; }
    public BigDecimal getAverageClosedHours() { return averageClosedHours; }
    public void setAverageClosedHours(BigDecimal value) { averageClosedHours = value; }
    public List<CountBucket> getEquipmentStatuses() { return equipmentStatuses; }
    public void setEquipmentStatuses(List<CountBucket> value) { equipmentStatuses = value; }
    public List<CountBucket> getOrderStatuses() { return orderStatuses; }
    public void setOrderStatuses(List<CountBucket> value) { orderStatuses = value; }
    public List<CountBucket> getFaultCategories() { return faultCategories; }
    public void setFaultCategories(List<CountBucket> value) { faultCategories = value; }
    public List<EquipmentReportCount> getTopEquipment() { return topEquipment; }
    public void setTopEquipment(List<EquipmentReportCount> value) { topEquipment = value; }
    public Date getGeneratedTime() { return generatedTime; }
    public void setGeneratedTime(Date value) { generatedTime = value; }
}
