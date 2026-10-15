package com.xiaokang.equipment.service;

import java.util.List;
import com.xiaokang.equipment.domain.Equipment;
import com.xiaokang.equipment.dto.EquipmentCreateRequest;
import com.xiaokang.equipment.dto.EquipmentEditRequest;
import com.xiaokang.equipment.dto.EquipmentQuery;
import com.xiaokang.equipment.dto.EquipmentStatusRequest;
import com.xiaokang.equipment.vo.EquipmentWorkOrder;

/** 设备台账业务入口。 */
public interface IEquipmentService
{
    List<Equipment> list(EquipmentQuery query, int pageNum, int pageSize);
    List<Equipment> export(EquipmentQuery query);
    Equipment get(Long id);
    Equipment create(EquipmentCreateRequest request);
    Equipment edit(Long id, EquipmentEditRequest request);
    Equipment changeStatus(Long id, EquipmentStatusRequest request);
    List<EquipmentWorkOrder> history(Long id, int pageNum, int pageSize);
}
