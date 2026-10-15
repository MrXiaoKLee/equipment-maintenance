package com.xiaokang.equipment.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import com.github.pagehelper.PageHelper;
import com.xiaokang.common.constant.HttpStatus;
import com.xiaokang.common.exception.ServiceException;
import com.xiaokang.common.utils.SecurityUtils;
import com.xiaokang.equipment.domain.Equipment;
import com.xiaokang.equipment.dto.EquipmentCreateRequest;
import com.xiaokang.equipment.dto.EquipmentEditRequest;
import com.xiaokang.equipment.dto.EquipmentQuery;
import com.xiaokang.equipment.dto.EquipmentStatusRequest;
import com.xiaokang.equipment.enums.EquipmentStatus;
import com.xiaokang.equipment.mapper.EquipmentMapper;
import com.xiaokang.equipment.service.IEquipmentService;
import com.xiaokang.equipment.vo.EquipmentWorkOrder;
import com.xiaokang.equipment.security.EquipmentActor;
import com.xiaokang.equipment.security.EquipmentAuthorization;
import com.xiaokang.system.service.ISysDictTypeService;

/** 设备校验、权限、版本更新与报废规则。 */
@Service
public class EquipmentServiceImpl implements IEquipmentService
{
    private final EquipmentMapper mapper;
    private final ISysDictTypeService dictService;
    private final EquipmentAuthorization authorization;

    public EquipmentServiceImpl(EquipmentMapper mapper, ISysDictTypeService dictService, EquipmentAuthorization authorization)
    {
        this.mapper = mapper;
        this.dictService = dictService;
        this.authorization = authorization;
    }

    @Override
    public List<Equipment> list(EquipmentQuery query, int pageNum, int pageSize)
    {
        requireReader();
        normalizeQuery(query);
        PageHelper.startPage(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
        try
        {
            return mapper.selectList(query);
        }
        finally
        {
            PageHelper.clearPage();
        }
    }

    @Override
    public List<Equipment> export(EquipmentQuery query)
    {
        requireManager();
        normalizeQuery(query);
        List<Equipment> rows = mapper.selectExportList(query);
        if (rows.size() > 1000)
        {
            throw badRequest("导出超过1000条，请缩小筛选范围");
        }
        Map<String, String> typeLabels = new HashMap<>();
        var types = dictService.selectDictDataByType("eq_equipment_type");
        if (types != null)
        {
            types.forEach(type -> typeLabels.put(type.getDictValue(), type.getDictLabel()));
        }
        rows.forEach(row -> row.setEquipmentTypeLabel(
                typeLabels.getOrDefault(row.getEquipmentType(), row.getEquipmentType())));
        return rows;
    }

    @Override
    public Equipment get(Long id)
    {
        requireReader();
        Equipment equipment = requireEquipment(mapper.selectById(id));
        equipment.setOpenWorkOrderCount(mapper.countOpenWorkOrders(id));
        return equipment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Equipment create(EquipmentCreateRequest request)
    {
        requireManager();
        String code = required(request.getEquipmentCode(), "设备编号", 32).toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_-]{1,32}"))
        {
            throw badRequest("设备编号只能使用英文字母、数字、连字符和下划线");
        }
        if (mapper.selectByCode(code) != null)
        {
            throw badRequest("设备编号已存在");
        }
        Equipment equipment = new Equipment();
        equipment.setEquipmentCode(code);
        fill(equipment, request.getEquipmentName(), request.getEquipmentType(), request.getModel(),
                request.getLocation(), request.getRemark());
        requireActiveType(equipment.getEquipmentType());
        equipment.setEquipmentStatus(EquipmentStatus.IN_SERVICE.name());
        equipment.setVersion(0);
        equipment.setCreateBy(SecurityUtils.getUsername());
        equipment.setCreateTime(new Date());
        try
        {
            mapper.insert(equipment);
        }
        catch (DuplicateKeyException ex)
        {
            throw badRequest("设备编号已存在");
        }
        return equipment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Equipment edit(Long id, EquipmentEditRequest request)
    {
        requireManager();
        Equipment equipment = requireEquipment(mapper.lockById(id));
        requireVersion(equipment, request.getVersion());
        String oldType = equipment.getEquipmentType();
        fill(equipment, request.getEquipmentName(), request.getEquipmentType(), request.getModel(),
                request.getLocation(), request.getRemark());
        if (!Objects.equals(oldType, equipment.getEquipmentType()))
        {
            requireActiveType(equipment.getEquipmentType());
        }
        equipment.setUpdateBy(SecurityUtils.getUsername());
        equipment.setUpdateTime(new Date());
        requireUpdated(mapper.update(equipment));
        equipment.setVersion(equipment.getVersion() + 1);
        return equipment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Equipment changeStatus(Long id, EquipmentStatusRequest request)
    {
        // 权限查询先于设备锁。等待设备锁后，工单检查必须看到刚提交的数据，不能沿用旧快照。
        requireManager();
        Equipment equipment = requireEquipment(mapper.lockById(id));
        requireVersion(equipment, request.getVersion());
        EquipmentStatus target = EquipmentStatus.parse(request.getEquipmentStatus());
        if (EquipmentStatus.RETIRED.name().equals(equipment.getEquipmentStatus()))
        {
            throw badRequest("报废设备不能再次调整使用状态");
        }
        if (target.name().equals(equipment.getEquipmentStatus()))
        {
            return equipment;
        }
        if (target == EquipmentStatus.RETIRED && mapper.countOpenWorkOrders(id) > 0)
        {
            throw badRequest("该设备存在未结束工单，不能报废");
        }
        equipment.setEquipmentStatus(target.name());
        equipment.setUpdateBy(SecurityUtils.getUsername());
        equipment.setUpdateTime(new Date());
        requireUpdated(mapper.updateStatus(equipment));
        equipment.setVersion(equipment.getVersion() + 1);
        return equipment;
    }

    @Override
    public List<EquipmentWorkOrder> history(Long id, int pageNum, int pageSize)
    {
        EquipmentActor actor = authorization.requireReader();
        requireEquipment(mapper.selectById(id));
        boolean all = actor.isManager();
        boolean reporter = actor.isReporter();
        boolean maintainer = actor.isMaintainer();
        // 先确认设备存在，再开始分页，避免设备查询消耗分页上下文。
        PageHelper.startPage(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
        try
        {
            return mapper.selectHistory(id, SecurityUtils.getUserId(), all, reporter, maintainer);
        }
        finally
        {
            PageHelper.clearPage();
        }
    }

    private void fill(Equipment equipment, String name, String type, String model, String location, String remark)
    {
        equipment.setEquipmentName(required(name, "设备名称", 100));
        equipment.setEquipmentType(required(type, "设备类型", 32));
        equipment.setModel(optional(model, "型号", 100));
        equipment.setLocation(required(location, "位置", 200));
        equipment.setRemark(optional(remark, "说明", 2000));
    }

    private void normalizeQuery(EquipmentQuery query)
    {
        query.setEquipmentCode(optional(query.getEquipmentCode(), "设备编号筛选", 32));
        query.setEquipmentName(optional(query.getEquipmentName(), "设备名称筛选", 100));
        query.setEquipmentType(optional(query.getEquipmentType(), "设备类型筛选", 32));
        query.setEquipmentStatus(optional(query.getEquipmentStatus(), "设备状态筛选", 32));
        query.setKeyword(optional(query.getKeyword(), "搜索关键词", 100));
        if (!query.getEquipmentStatus().isEmpty())
        {
            EquipmentStatus.parse(query.getEquipmentStatus());
        }
    }

    private void requireActiveType(String type)
    {
        var types = dictService.selectDictDataByType("eq_equipment_type");
        boolean found = types != null && types.stream()
                .anyMatch(dict -> type.equals(dict.getDictValue()) && "0".equals(dict.getStatus()));
        if (!found)
        {
            throw badRequest("设备类型不存在或已停用");
        }
    }

    private Equipment requireEquipment(Equipment equipment)
    {
        if (equipment == null)
        {
            throw new ServiceException("设备不存在", HttpStatus.NOT_FOUND);
        }
        return equipment;
    }

    private void requireVersion(Equipment equipment, Integer version)
    {
        if (version == null || version < 0)
        {
            throw badRequest("缺少有效的设备版本号");
        }
        if (!version.equals(equipment.getVersion()))
        {
            throw new ServiceException("设备已更新，请刷新后重试", HttpStatus.CONFLICT);
        }
    }

    private void requireUpdated(int rows)
    {
        if (rows != 1)
        {
            throw new ServiceException("设备已更新，请刷新后重试", HttpStatus.CONFLICT);
        }
    }

    private void requireReader()
    {
        authorization.requireReader();
    }

    private void requireManager()
    {
        authorization.requireManager();
    }

    private String required(String value, String label, int max)
    {
        String text = optional(value, label, max);
        if (text.isEmpty())
        {
            throw badRequest(label + "不能为空");
        }
        return text;
    }

    private String optional(String value, String label, int max)
    {
        String text = value == null ? "" : value.trim();
        if (text.length() > max)
        {
            throw badRequest(label + "不能超过" + max + "个字符");
        }
        return text;
    }

    private ServiceException badRequest(String message)
    {
        return new ServiceException(message, HttpStatus.BAD_REQUEST);
    }
}
