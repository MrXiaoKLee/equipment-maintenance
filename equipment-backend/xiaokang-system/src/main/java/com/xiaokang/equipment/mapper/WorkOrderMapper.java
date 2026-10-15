package com.xiaokang.equipment.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.xiaokang.equipment.domain.WorkOrder;
import com.xiaokang.equipment.domain.WorkOrderRecord;
import com.xiaokang.equipment.dto.WorkOrderQuery;
import com.xiaokang.equipment.security.EquipmentActor;
import com.xiaokang.equipment.vo.MaintainerOption;

public interface WorkOrderMapper
{
    List<WorkOrder> selectList(@Param("query") WorkOrderQuery query, @Param("actor") EquipmentActor actor);
    List<WorkOrder> selectExportList(@Param("query") WorkOrderQuery query, @Param("actor") EquipmentActor actor);
    WorkOrder selectById(Long id);
    WorkOrder lockById(Long id);
    int countVisibleOpen(@Param("equipmentId") Long equipmentId, @Param("actor") EquipmentActor actor);
    int insert(WorkOrder order);
    int assign(WorkOrder order);
    int cancel(WorkOrder order);
    int updateWorkflow(@Param("order") WorkOrder order, @Param("fromStatus") String fromStatus,
            @Param("handlerId") Long handlerId);
    int insertRecord(WorkOrderRecord record);
    List<WorkOrderRecord> selectRecords(Long id);
    List<MaintainerOption> selectMaintainers(@Param("keyword") String keyword);
    MaintainerOption lockMaintainer(Long id);
}
