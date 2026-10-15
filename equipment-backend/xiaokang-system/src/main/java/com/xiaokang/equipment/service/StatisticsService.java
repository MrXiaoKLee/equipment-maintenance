package com.xiaokang.equipment.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.xiaokang.common.exception.ServiceException;
import com.xiaokang.equipment.dto.ReportingPeriod;
import com.xiaokang.equipment.dto.StatisticsQuery;
import com.xiaokang.equipment.enums.EquipmentStatus;
import com.xiaokang.equipment.enums.WorkOrderStatus;
import com.xiaokang.equipment.mapper.StatisticsMapper;
import com.xiaokang.equipment.security.EquipmentAuthorization;
import com.xiaokang.equipment.vo.CountBucket;
import com.xiaokang.equipment.vo.StatisticsOverview;
import com.xiaokang.system.service.ISysDictTypeService;

@Service
public class StatisticsService
{
    private final StatisticsMapper mapper;
    private final EquipmentAuthorization authorization;
    private final ISysDictTypeService dictionaries;

    public StatisticsService(StatisticsMapper mapper, EquipmentAuthorization authorization, ISysDictTypeService dictionaries)
    {
        this.mapper = mapper;
        this.authorization = authorization;
        this.dictionaries = dictionaries;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public StatisticsOverview overview(StatisticsQuery query)
    {
        authorization.requireManager();
        if (query.getEquipmentId() != null && query.getEquipmentId() <= 0)
        {
            throw new ServiceException("请选择有效的设备", 400);
        }
        query.setPeriod(ReportingPeriod.parse(query.getBeginDate(), query.getEndDate()));
        StatisticsOverview result = mapper.selectSummary(query);
        result.setEquipmentStatuses(fillStatuses(mapper.selectEquipmentStatuses(),
                java.util.Arrays.stream(EquipmentStatus.values()).map(Enum::name).toList(), "eq_equipment_status"));
        result.setEquipmentTotal(result.getEquipmentStatuses().stream().mapToLong(CountBucket::getCount).sum());
        result.setOrderStatuses(fillStatuses(mapper.selectOrderStatuses(query),
                java.util.Arrays.stream(WorkOrderStatus.values()).map(Enum::name).toList(), "eq_work_order_status"));
        result.setFaultCategories(mapper.selectFaultCategories(query));
        Map<String, String> labels = labels("eq_fault_category");
        result.getFaultCategories().forEach(row -> row.setLabel(labels.getOrDefault(row.getCode(), row.getCode())));
        result.setTopEquipment(mapper.selectTopEquipment(query));
        result.setGeneratedTime(new Date());
        return result;
    }

    private List<CountBucket> fillStatuses(List<CountBucket> rows, List<String> codes, String dictionary)
    {
        Map<String, String> labels = labels(dictionary);
        Map<String, Long> counts = new HashMap<>();
        rows.forEach(row -> counts.put(row.getCode(), row.getCount()));
        List<CountBucket> result = new ArrayList<>();
        codes.forEach(code -> result.add(new CountBucket(code, labels.getOrDefault(code, code), counts.getOrDefault(code, 0L))));
        return result;
    }

    private Map<String, String> labels(String type)
    {
        Map<String, String> result = new HashMap<>();
        var entries = dictionaries.selectDictDataByType(type);
        if (entries != null) entries.forEach(entry -> result.put(entry.getDictValue(), entry.getDictLabel()));
        return result;
    }
}
