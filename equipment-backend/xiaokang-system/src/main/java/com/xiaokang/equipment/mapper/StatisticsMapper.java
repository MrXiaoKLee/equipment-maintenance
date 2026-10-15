package com.xiaokang.equipment.mapper;

import java.util.List;
import com.xiaokang.equipment.dto.StatisticsQuery;
import com.xiaokang.equipment.vo.CountBucket;
import com.xiaokang.equipment.vo.EquipmentReportCount;
import com.xiaokang.equipment.vo.StatisticsOverview;

public interface StatisticsMapper
{
    List<CountBucket> selectEquipmentStatuses();
    StatisticsOverview selectSummary(StatisticsQuery query);
    List<CountBucket> selectOrderStatuses(StatisticsQuery query);
    List<CountBucket> selectFaultCategories(StatisticsQuery query);
    List<EquipmentReportCount> selectTopEquipment(StatisticsQuery query);
}
