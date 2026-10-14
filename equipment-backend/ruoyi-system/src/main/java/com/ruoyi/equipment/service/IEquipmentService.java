package com.ruoyi.equipment.service;

import java.util.List;
import com.ruoyi.equipment.domain.Equipment;
import com.ruoyi.equipment.dto.EquipmentCreateRequest;
import com.ruoyi.equipment.dto.EquipmentEditRequest;
import com.ruoyi.equipment.dto.EquipmentQuery;
import com.ruoyi.equipment.dto.EquipmentStatusRequest;
import com.ruoyi.equipment.vo.EquipmentWorkOrder;

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
