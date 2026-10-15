package com.ruoyi.equipment.mapper;

import java.util.List;
import com.ruoyi.equipment.dto.StatisticsQuery;
import com.ruoyi.equipment.vo.CountBucket;
import com.ruoyi.equipment.vo.EquipmentReportCount;
import com.ruoyi.equipment.vo.StatisticsOverview;

public interface StatisticsMapper
{
    List<CountBucket> selectEquipmentStatuses();
    StatisticsOverview selectSummary(StatisticsQuery query);
    List<CountBucket> selectOrderStatuses(StatisticsQuery query);
    List<CountBucket> selectFaultCategories(StatisticsQuery query);
    List<EquipmentReportCount> selectTopEquipment(StatisticsQuery query);
}
