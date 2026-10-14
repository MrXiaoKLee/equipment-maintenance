package com.ruoyi.equipment.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.equipment.domain.Equipment;
import com.ruoyi.equipment.dto.EquipmentQuery;
import com.ruoyi.equipment.vo.EquipmentWorkOrder;

/** 设备持久化及只读工单关联查询。 */
public interface EquipmentMapper
{
    List<Equipment> selectList(EquipmentQuery query);
    List<Equipment> selectExportList(EquipmentQuery query);
    Equipment selectById(Long id);
    Equipment lockById(Long id);
    Equipment selectByCode(String code);
    int insert(Equipment equipment);
    int update(Equipment equipment);
    int updateStatus(Equipment equipment);
    int countOpenWorkOrders(Long id);
    List<EquipmentWorkOrder> selectHistory(@Param("id") Long id, @Param("userId") Long userId,
            @Param("all") boolean all, @Param("reporter") boolean reporter, @Param("maintainer") boolean maintainer);
}
