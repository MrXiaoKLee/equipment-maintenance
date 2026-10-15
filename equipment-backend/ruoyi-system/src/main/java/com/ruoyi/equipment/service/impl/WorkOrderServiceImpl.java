package com.ruoyi.equipment.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.equipment.domain.Equipment;
import com.ruoyi.equipment.domain.WorkOrder;
import com.ruoyi.equipment.domain.WorkOrderRecord;
import com.ruoyi.equipment.dto.WorkOrderAssignRequest;
import com.ruoyi.equipment.dto.WorkOrderCancelRequest;
import com.ruoyi.equipment.dto.WorkOrderCreateRequest;
import com.ruoyi.equipment.dto.WorkOrderQuery;
import com.ruoyi.equipment.dto.WorkOrderProcessRequest;
import com.ruoyi.equipment.dto.WorkOrderSubmitReviewRequest;
import com.ruoyi.equipment.dto.WorkOrderReviewRequest;
import com.ruoyi.equipment.enums.EquipmentStatus;
import com.ruoyi.equipment.enums.WorkOrderAction;
import com.ruoyi.equipment.enums.WorkOrderPriority;
import com.ruoyi.equipment.enums.WorkOrderStatus;
import com.ruoyi.equipment.mapper.EquipmentMapper;
import com.ruoyi.equipment.mapper.WorkOrderMapper;
import com.ruoyi.equipment.security.EquipmentActor;
import com.ruoyi.equipment.security.EquipmentAuthorization;
import com.ruoyi.equipment.service.IWorkOrderService;
import com.ruoyi.equipment.vo.MaintainerOption;
import com.ruoyi.system.service.ISysDictTypeService;

/** 工单动作在服务端校验身份、状态和版本，主表与追加式记录同一事务提交。 */
@Service
public class WorkOrderServiceImpl implements IWorkOrderService
{
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);
    private final WorkOrderMapper mapper;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentAuthorization authorization;
    private final ISysDictTypeService dictionaries;

    public WorkOrderServiceImpl(WorkOrderMapper mapper, EquipmentMapper equipmentMapper,
            EquipmentAuthorization authorization, ISysDictTypeService dictionaries)
    {
        this.mapper = mapper;
        this.equipmentMapper = equipmentMapper;
        this.authorization = authorization;
        this.dictionaries = dictionaries;
    }

    @Override
    public List<WorkOrder> list(WorkOrderQuery query, int pageNum, int pageSize)
    {
        EquipmentActor actor = authorization.requireReader();
        normalizeQuery(query, actor);
        PageHelper.startPage(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
        try
        {
            return mapper.selectList(query, actor);
        }
        finally
        {
            PageHelper.clearPage();
        }
    }

    @Override
    public WorkOrder get(Long id)
    {
        EquipmentActor actor = authorization.requireReader();
        return requireVisible(mapper.selectById(id), actor);
    }

    @Override
    public List<WorkOrderRecord> records(Long id)
    {
        get(id);
        return mapper.selectRecords(id);
    }

    @Override
    public List<MaintainerOption> maintainers(String keyword, int pageNum, int pageSize)
    {
        authorization.requireManager();
        String search = text(keyword, "维护人员关键词", 64, false);
        PageHelper.startPage(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
        try
        {
            return mapper.selectMaintainers(search);
        }
        finally
        {
            PageHelper.clearPage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public WorkOrder create(WorkOrderCreateRequest request)
    {
        EquipmentActor actor = authorization.requireReporter();
        requireId(request.getEquipmentId(), "设备");
        WorkOrder order = new WorkOrder();
        order.setEquipmentId(request.getEquipmentId());
        order.setTitle(text(request.getTitle(), "标题", 100, true));
        order.setSymptom(text(request.getSymptom(), "故障现象", 2000, true));
        order.setFaultCategory(text(request.getFaultCategory(), "故障类别", 32, true));
        order.setPriority(WorkOrderPriority.parse(text(request.getPriority(), "优先级", 16, true)).name());
        requireDictionary("eq_fault_category", order.getFaultCategory(), "故障类别");
        requireDictionary("eq_priority", order.getPriority(), "优先级");
        order.setOccurredTime(parseOccurredTime(request.getOccurredTime()));
        // 所有写工单的动作先设备、后工单。与报废检查使用同一设备行锁。
        Equipment equipment = equipmentMapper.lockById(order.getEquipmentId());
        if (equipment == null)
        {
            throw new ServiceException("设备不存在", 404);
        }
        if (!EquipmentStatus.IN_SERVICE.name().equals(equipment.getEquipmentStatus()))
        {
            throw new ServiceException("只能为在用设备提交报修", 400);
        }
        if (mapper.countVisibleOpen(order.getEquipmentId(), actor) > 0
                && !Boolean.TRUE.equals(request.getConfirmExisting()))
        {
            throw new ServiceException("当前可见范围内已有未结束工单，请查看并确认继续报修", 409);
        }
        Date now = new Date();
        order.setWorkOrderNo("WO-" + UUID.randomUUID().toString().replace("-", ""));
        order.setReporterId(actor.getUserId());
        order.setStatus(WorkOrderStatus.WAITING.name());
        order.setVersion(0);
        order.setCreateBy(actor.getUserName());
        order.setCreateTime(now);
        requireWritten(mapper.insert(order));
        appendRecord(order, WorkOrderAction.CREATE, null, "提交报修", actor, now);
        return mapper.selectById(order.getWorkOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public WorkOrder assign(Long id, WorkOrderAssignRequest request)
    {
        EquipmentActor actor = authorization.requireManager();
        WorkOrder order = lockVisible(id, actor);
        requireWaitingVersion(order, request.getVersion());
        requireId(request.getHandlerId(), "维护人员");
        String note = text(request.getDescription(), "分配说明", 1800, false);
        MaintainerOption handler = mapper.lockMaintainer(request.getHandlerId());
        if (handler == null)
        {
            throw new ServiceException("维护人员不存在、已停用或不具有有效维护角色", 400);
        }
        Date now = new Date();
        order.setHandlerId(handler.getUserId());
        order.setStatus(WorkOrderStatus.PROCESSING.name());
        order.setAssignedTime(now);
        setUpdated(order, actor, now);
        requireWritten(mapper.assign(order));
        String handlerName = handler.getNickName() == null ? handler.getUserName() : handler.getNickName();
        appendRecord(order, WorkOrderAction.ASSIGN, WorkOrderStatus.WAITING,
                "分配给 " + handlerName + "（" + handler.getUserName() + "）" + (note.isEmpty() ? "" : "：" + note), actor, now);
        return mapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public WorkOrder cancel(Long id, WorkOrderCancelRequest request)
    {
        EquipmentActor actor = authorization.requireReader();
        WorkOrder order = lockVisible(id, actor);
        if (!actor.isManager() && !(actor.isReporter() && Objects.equals(actor.getUserId(), order.getReporterId())))
        {
            throw new ServiceException("只有本人报修或设备管理人员可以取消", 403);
        }
        requireWaitingVersion(order, request.getVersion());
        String reason = text(request.getReason(), "取消原因", 2000, true);
        Date now = new Date();
        order.setStatus(WorkOrderStatus.CANCELLED.name());
        order.setCancelledTime(now);
        setUpdated(order, actor, now);
        requireWritten(mapper.cancel(order));
        appendRecord(order, WorkOrderAction.CANCEL, WorkOrderStatus.WAITING, reason, actor, now);
        return mapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public WorkOrder process(Long id, WorkOrderProcessRequest request)
    {
        EquipmentActor actor = authorization.requireReader();
        WorkOrder order = lockVisible(id, actor);
        requireHandler(order, actor);
        requireVersionStatus(order, request.getVersion(), WorkOrderStatus.PROCESSING);
        String description = text(request.getDescription(), "处理记录", 2000, true);
        Date now = new Date();
        setUpdated(order, actor, now);
        requireWritten(mapper.updateWorkflow(order, WorkOrderStatus.PROCESSING.name(), actor.getUserId()));
        appendRecord(order, WorkOrderAction.PROCESS_NOTE, WorkOrderStatus.PROCESSING, description, actor, now);
        return mapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public WorkOrder submitReview(Long id, WorkOrderSubmitReviewRequest request)
    {
        EquipmentActor actor = authorization.requireReader();
        WorkOrder order = lockVisible(id, actor);
        requireHandler(order, actor);
        requireVersionStatus(order, request.getVersion(), WorkOrderStatus.PROCESSING);
        String cause = text(request.getCauseAnalysis(), "原因判断", 2000, true);
        String measures = text(request.getMeasures(), "处理措施", 2000, true);
        String result = text(request.getResultDescription(), "处理结果", 2000, true);
        Date now = new Date();
        order.setStatus(WorkOrderStatus.PENDING_REVIEW.name());
        order.setLastSubmitTime(now);
        setUpdated(order, actor, now);
        requireWritten(mapper.updateWorkflow(order, WorkOrderStatus.PROCESSING.name(), actor.getUserId()));
        WorkOrderRecord record = newRecord(order, WorkOrderAction.SUBMIT_REVIEW,
                WorkOrderStatus.PROCESSING, "提交处理结果，等待复核", actor, now);
        record.setCauseAnalysis(cause);
        record.setMeasures(measures);
        record.setResultDescription(result);
        requireWritten(mapper.insertRecord(record));
        return mapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public WorkOrder review(Long id, WorkOrderReviewRequest request)
    {
        EquipmentActor actor = authorization.requireManager();
        WorkOrder order = lockVisible(id, actor);
        requireVersionStatus(order, request.getVersion(), WorkOrderStatus.PENDING_REVIEW);
        String decision = text(request.getDecision(), "复核决定", 16, true);
        if (!"APPROVE".equals(decision) && !"REJECT".equals(decision))
        {
            throw new ServiceException("复核决定只能为APPROVE或REJECT", 400);
        }
        boolean approve = "APPROVE".equals(decision);
        String description = text(request.getDescription(), approve ? "复核结论" : "退回原因", 2000, true);
        Date now = new Date();
        order.setStatus(approve ? WorkOrderStatus.CLOSED.name() : WorkOrderStatus.PROCESSING.name());
        if (approve)
        {
            order.setClosedTime(now);
        }
        setUpdated(order, actor, now);
        requireWritten(mapper.updateWorkflow(order, WorkOrderStatus.PENDING_REVIEW.name(), null));
        appendRecord(order, approve ? WorkOrderAction.REVIEW_APPROVE : WorkOrderAction.REVIEW_REJECT,
                WorkOrderStatus.PENDING_REVIEW, description, actor, now);
        return mapper.selectById(id);
    }

    private void requireHandler(WorkOrder order, EquipmentActor actor)
    {
        if (!actor.isMaintainer() || !Objects.equals(order.getHandlerId(), actor.getUserId()))
        {
            throw new ServiceException("只有当前负责的维护人员可以填写处理记录或提交复核", 403);
        }
    }

    private WorkOrder lockVisible(Long id, EquipmentActor actor)
    {
        WorkOrder initial = requireVisible(mapper.selectById(id), actor);
        equipmentMapper.lockById(initial.getEquipmentId());
        return requireVisible(mapper.lockById(id), actor);
    }

    private WorkOrder requireVisible(WorkOrder order, EquipmentActor actor)
    {
        if (order == null || !actor.canView(order))
        {
            throw new ServiceException("工单不存在", 404);
        }
        return order;
    }

    private void requireWaitingVersion(WorkOrder order, Integer version)
    {
        requireVersionStatus(order, version, WorkOrderStatus.WAITING);
    }

    private void requireVersionStatus(WorkOrder order, Integer version, WorkOrderStatus expected)
    {
        if (version == null || version < 0)
        {
            throw new ServiceException("缺少有效的工单版本号", 400);
        }
        if (!version.equals(order.getVersion()))
        {
            throw new ServiceException("工单已更新，请刷新后重试", 409);
        }
        if (!expected.name().equals(order.getStatus()))
        {
            throw new ServiceException("当前工单状态不允许该操作，请刷新后查看", 400);
        }
    }

    private void appendRecord(WorkOrder order, WorkOrderAction action, WorkOrderStatus from,
            String description, EquipmentActor actor, Date time)
    {
        requireWritten(mapper.insertRecord(newRecord(order, action, from, description, actor, time)));
    }

    private WorkOrderRecord newRecord(WorkOrder order, WorkOrderAction action, WorkOrderStatus from,
            String description, EquipmentActor actor, Date time)
    {
        WorkOrderRecord record = new WorkOrderRecord();
        record.setWorkOrderId(order.getWorkOrderId());
        record.setActionType(action.name());
        record.setFromStatus(from == null ? null : from.name());
        record.setToStatus(order.getStatus());
        record.setDescription(description);
        record.setActorId(actor.getUserId());
        record.setActorName(actor.getDisplayName());
        record.setCreateTime(time);
        return record;
    }

    private void setUpdated(WorkOrder order, EquipmentActor actor, Date now)
    {
        order.setUpdateBy(actor.getUserName());
        order.setUpdateTime(now);
    }

    private void requireWritten(int rows)
    {
        if (rows != 1)
        {
            throw new ServiceException("工单已更新，请刷新后重试", 409);
        }
    }

    private void requireDictionary(String type, String value, String label)
    {
        var entries = dictionaries.selectDictDataByType(type);
        if (entries == null || entries.stream().noneMatch(entry -> value.equals(entry.getDictValue())
                && "0".equals(entry.getStatus())))
        {
            throw new ServiceException(label + "不存在或已停用", 400);
        }
    }

    private Date parseOccurredTime(String value)
    {
        try
        {
            LocalDateTime time = LocalDateTime.parse(text(value, "发生时间", 19, true), TIME);
            Date parsed = Date.from(time.atZone(ZONE).toInstant());
            if (time.getYear() < 1000 || parsed.after(new Date()))
            {
                throw new ServiceException("发生时间应为有效时间，且不能晚于当前时间", 400);
            }
            return parsed;
        }
        catch (DateTimeParseException ex)
        {
            throw new ServiceException("发生时间格式应为yyyy-MM-dd HH:mm:ss", 400);
        }
    }

    private void normalizeQuery(WorkOrderQuery query, EquipmentActor actor)
    {
        query.setWorkOrderNo(text(query.getWorkOrderNo(), "工单编号", 40, false));
        query.setStatus(text(query.getStatus(), "状态", 32, false));
        query.setFaultCategory(text(query.getFaultCategory(), "故障类别", 32, false));
        query.setPriority(text(query.getPriority(), "优先级", 16, false));
        query.setReporterName(text(query.getReporterName(), "报修人筛选", 64, false));
        query.setHandlerName(text(query.getHandlerName(), "维护人员筛选", 64, false));
        if (!query.getStatus().isEmpty()) WorkOrderStatus.parse(query.getStatus());
        if (!query.getPriority().isEmpty()) WorkOrderPriority.parse(query.getPriority());
        query.setBeginTime(parseDate(query.getBeginDate(), false));
        query.setEndTime(parseDate(query.getEndDate(), true));
        if (query.getBeginTime() != null && query.getEndTime() != null
                && !query.getBeginTime().before(query.getEndTime()))
        {
            throw new ServiceException("开始日期不能晚于结束日期", 400);
        }
        if (!actor.isManager())
        {
            query.setReporterId(null);
            query.setHandlerId(null);
            query.setReporterName("");
            query.setHandlerName("");
        }
    }

    private Date parseDate(String value, boolean end)
    {
        if (value == null || value.isBlank()) return null;
        try
        {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            if (date.getYear() < 1000 || date.getYear() > 9998) throw new DateTimeParseException("range", value, 0);
            return Date.from((end ? date.plusDays(1) : date).atStartOfDay(ZONE).toInstant());
        }
        catch (DateTimeParseException ex)
        {
            throw new ServiceException("日期应为yyyy-MM-dd格式的有效日期", 400);
        }
    }

    private void requireId(Long value, String label)
    {
        if (value == null || value <= 0) throw new ServiceException("请选择有效的" + label, 400);
    }

    private String text(String value, String label, int max, boolean required)
    {
        String result = value == null ? "" : value.trim();
        if (required && result.isEmpty()) throw new ServiceException(label + "不能为空", 400);
        if (result.length() > max) throw new ServiceException(label + "不能超过" + max + "个字符", 400);
        return result;
    }
}
