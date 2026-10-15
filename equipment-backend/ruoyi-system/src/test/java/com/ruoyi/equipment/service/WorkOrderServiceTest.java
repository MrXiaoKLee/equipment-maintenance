package com.ruoyi.equipment.service;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.equipment.domain.Equipment;
import com.ruoyi.equipment.domain.WorkOrder;
import com.ruoyi.equipment.domain.WorkOrderRecord;
import com.ruoyi.equipment.dto.WorkOrderAssignRequest;
import com.ruoyi.equipment.dto.WorkOrderCancelRequest;
import com.ruoyi.equipment.dto.WorkOrderCreateRequest;
import com.ruoyi.equipment.dto.WorkOrderQuery;
import com.ruoyi.equipment.mapper.EquipmentMapper;
import com.ruoyi.equipment.mapper.WorkOrderMapper;
import com.ruoyi.equipment.security.EquipmentActor;
import com.ruoyi.equipment.security.EquipmentAuthorization;
import com.ruoyi.equipment.service.impl.WorkOrderServiceImpl;
import com.ruoyi.equipment.vo.MaintainerOption;
import com.ruoyi.system.service.ISysDictTypeService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkOrderServiceTest
{
    private WorkOrderMapper orders;
    private EquipmentMapper equipment;
    private EquipmentAuthorization authorization;
    private ISysDictTypeService dictionaries;
    private WorkOrderServiceImpl service;
    private EquipmentActor reporter;
    private WorkOrder order;

    @BeforeEach
    void setup()
    {
        orders = mock(WorkOrderMapper.class);
        equipment = mock(EquipmentMapper.class);
        authorization = mock(EquipmentAuthorization.class);
        dictionaries = mock(ISysDictTypeService.class);
        service = new WorkOrderServiceImpl(orders, equipment, authorization, dictionaries);
        reporter = new EquipmentActor(100L, "reporter", "模拟报修人", false, true, false);
        when(authorization.requireReader()).thenReturn(reporter);
        when(authorization.requireReporter()).thenReturn(reporter);
        when(authorization.requireManager()).thenReturn(new EquipmentActor(1L, "admin", "管理员", true, true, true));
        when(dictionaries.selectDictDataByType(anyString())).thenAnswer(call -> {
            SysDictData entry = new SysDictData();
            entry.setDictValue("eq_priority".equals(call.getArgument(0)) ? "NORMAL" : "MECHANICAL");
            entry.setStatus("0");
            return List.of(entry);
        });
        Equipment device = new Equipment();
        device.setEquipmentStatus("IN_SERVICE");
        when(equipment.lockById(7L)).thenReturn(device);
        order = new WorkOrder();
        order.setWorkOrderId(9L);
        order.setWorkOrderNo("WO-demo");
        order.setEquipmentId(7L);
        order.setReporterId(100L);
        order.setStatus("WAITING");
        order.setVersion(0);
        when(orders.selectById(9L)).thenAnswer(call -> order);
        when(orders.lockById(9L)).thenAnswer(call -> order);
        when(orders.insert(any())).thenAnswer(call -> {
            order = call.getArgument(0);
            order.setWorkOrderId(9L);
            return 1;
        });
        when(orders.insertRecord(any())).thenReturn(1);
        when(orders.assign(any())).thenReturn(1);
        when(orders.cancel(any())).thenReturn(1);
    }

    @AfterEach
    void cleanup()
    {
        PageHelper.clearPage();
    }

    @Test
    void createsServerOwnedIdentityAndInitialRecord()
    {
        WorkOrder created = service.create(createRequest());
        assertTrue(created.getWorkOrderNo().matches("WO-[a-f0-9]{32}"));
        assertEquals(100L, created.getReporterId());
        assertEquals("WAITING", created.getStatus());
        assertEquals("模拟异响", created.getTitle());
        assertEquals("reporter", created.getCreateBy());
        assertEquals(0, created.getVersion());
        var record = org.mockito.ArgumentCaptor.forClass(WorkOrderRecord.class);
        verify(orders).insertRecord(record.capture());
        assertEquals("CREATE", record.getValue().getActionType());
        assertNull(record.getValue().getFromStatus());
        assertEquals("WAITING", record.getValue().getToStatus());
        assertEquals("模拟报修人", record.getValue().getActorName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OUT_OF_SERVICE", "RETIRED"})
    void rejectsUnavailableDevicesWithoutCreatingOrderOrRecord(String status)
    {
        Equipment device = new Equipment();
        device.setEquipmentStatus(status);
        when(equipment.lockById(7L)).thenReturn(device);
        error(400, () -> service.create(createRequest()));
        verify(orders, never()).insert(any());
        verify(orders, never()).insertRecord(any());
    }

    @Test
    void rejectsFutureAndInvalidCalendarTime()
    {
        WorkOrderCreateRequest request = createRequest();
        request.setOccurredTime("2999-01-01 00:00:00");
        error(400, () -> service.create(request));
        request.setOccurredTime("2026-02-30 00:00:00");
        error(400, () -> service.create(request));
        verify(orders, never()).insert(any());
    }

    @Test
    void rejectsBlankAndOverlongContent()
    {
        WorkOrderCreateRequest request = createRequest();
        request.setTitle("  ");
        error(400, () -> service.create(request));
        request.setTitle("正常标题");
        request.setSymptom("x".repeat(2001));
        error(400, () -> service.create(request));
        verify(orders, never()).insert(any());
    }

    @Test
    void rejectsDisabledCategoryAndUnsupportedPriority()
    {
        when(dictionaries.selectDictDataByType("eq_fault_category")).thenReturn(null);
        error(400, () -> service.create(createRequest()));
        WorkOrderCreateRequest request = createRequest();
        request.setPriority("URGENT");
        error(400, () -> service.create(request));
    }

    @Test
    void requiresAcknowledgementOnlyForVisibleOpenOrders()
    {
        when(orders.countVisibleOpen(7L, reporter)).thenReturn(1);
        error(409, () -> service.create(createRequest()));
        verify(orders, never()).insert(any());
        WorkOrderCreateRequest request = createRequest();
        request.setConfirmExisting(true);
        assertEquals("WAITING", service.create(request).getStatus());
    }

    @Test
    void nonReporterCannotCreateEvenWithForgedButtonPermission()
    {
        when(authorization.requireReporter()).thenThrow(new ServiceException("无报修权限", 403));
        error(403, () -> service.create(createRequest()));
        verifyNoInteractions(orders, equipment);
    }

    @Test
    void outOfScopeOrderAndTimelineAreNotFound()
    {
        order.setReporterId(200L);
        error(404, () -> service.get(9L));
        error(404, () -> service.records(9L));
        verify(orders, never()).selectRecords(any());
    }

    @Test
    void assignedMaintainerAndMultiRoleActorCanRead()
    {
        order.setReporterId(200L);
        order.setHandlerId(100L);
        when(authorization.requireReader()).thenReturn(new EquipmentActor(100L,"m","维护",false,false,true));
        assertSame(order, service.get(9L));
        when(authorization.requireReader()).thenReturn(new EquipmentActor(100L,"multi","多角色",false,true,true));
        assertSame(order, service.get(9L));
    }

    @Test
    void assignWritesHandlerStateTimestampAndAuditTogether()
    {
        MaintainerOption handler = new MaintainerOption();
        handler.setUserId(300L); handler.setNickName("模拟维护员"); handler.setUserName("maintainer");
        when(orders.lockMaintainer(300L)).thenReturn(handler);
        service.assign(9L, assignRequest());
        assertEquals(300L, order.getHandlerId());
        assertEquals("PROCESSING", order.getStatus());
        assertNotNull(order.getAssignedTime());
        var record = org.mockito.ArgumentCaptor.forClass(WorkOrderRecord.class);
        verify(orders).insertRecord(record.capture());
        assertEquals("ASSIGN", record.getValue().getActionType());
        assertTrue(record.getValue().getDescription().contains("模拟维护员"));
        var sequence = inOrder(equipment, orders);
        sequence.verify(equipment).lockById(7L);
        sequence.verify(orders).lockById(9L);
        sequence.verify(orders).assign(any());
        sequence.verify(orders).insertRecord(any());
    }

    @Test
    void rejectsDisabledOrNonMaintainerTarget()
    {
        when(orders.lockMaintainer(300L)).thenReturn(null);
        error(400, () -> service.assign(9L, assignRequest()));
        verify(orders, never()).assign(any());
        verify(orders, never()).insertRecord(any());
    }

    @Test
    void staleVersionAndProcessingStateCannotAssign()
    {
        order.setVersion(1);
        error(409, () -> service.assign(9L, assignRequest()));
        order.setVersion(0); order.setStatus("PROCESSING");
        error(400, () -> service.assign(9L, assignRequest()));
        verify(orders, never()).assign(any());
    }

    @Test
    void cancellationRequiresReasonAndAddsTerminalRecord()
    {
        WorkOrderCancelRequest request = cancelRequest();
        request.setReason("  ");
        error(400, () -> service.cancel(9L, request));
        request.setReason("模拟误报取消");
        service.cancel(9L, request);
        assertEquals("CANCELLED", order.getStatus());
        assertNotNull(order.getCancelledTime());
        var record = org.mockito.ArgumentCaptor.forClass(WorkOrderRecord.class);
        verify(orders).insertRecord(record.capture());
        assertEquals("CANCEL", record.getValue().getActionType());
        assertEquals("模拟误报取消", record.getValue().getDescription());
    }

    @Test
    void maintainerCannotCancelEvenWhenAssignedAndVisible()
    {
        order.setHandlerId(100L);
        when(authorization.requireReader()).thenReturn(new EquipmentActor(100L,"m","维护",false,false,true));
        error(403, () -> service.cancel(9L, cancelRequest()));
        verify(orders, never()).cancel(any());
    }

    @Test
    void failedConditionalUpdateDoesNotAppendRecord()
    {
        when(orders.cancel(any())).thenReturn(0);
        error(409, () -> service.cancel(9L, cancelRequest()));
        verify(orders, never()).insertRecord(any());
    }

    @Test
    void queryNormalizesDateBoundaryAndCannotReplaceScopeWithUserFilters()
    {
        WorkOrderQuery query = new WorkOrderQuery();
        query.setBeginDate("2026-01-01"); query.setEndDate("2026-01-01");
        query.setReporterId(200L); query.setHandlerId(300L); query.setReporterName("其他人员");
        when(orders.selectList(query, reporter)).thenAnswer(call -> {
            assertEquals(100, PageHelper.getLocalPage().getPageSize());
            return List.of();
        });
        service.list(query, 1, 1000);
        assertNull(query.getReporterId()); assertNull(query.getHandlerId());
        assertEquals("", query.getReporterName());
        assertEquals(86400000L, query.getEndTime().getTime() - query.getBeginTime().getTime());
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void reversedDateRangeIsRejectedBeforeQuery()
    {
        WorkOrderQuery query = new WorkOrderQuery();
        query.setBeginDate("2026-01-02"); query.setEndDate("2026-01-01");
        error(400, () -> service.list(query, 1, 10));
        verify(orders, never()).selectList(any(), any());
        assertNull(PageHelper.getLocalPage());
    }

    private WorkOrderCreateRequest createRequest()
    {
        WorkOrderCreateRequest request = new WorkOrderCreateRequest();
        request.setEquipmentId(7L); request.setTitle(" 模拟异响 "); request.setSymptom("模拟持续异响");
        request.setOccurredTime("2020-01-01 09:00:00"); request.setFaultCategory("MECHANICAL"); request.setPriority("NORMAL");
        return request;
    }

    @Test
    void exportUsesActorScopeAndAllMatchingRowsWithoutPagination()
    {
        WorkOrderQuery query = new WorkOrderQuery(); query.setReporterId(200L); query.setBeginDate("2026-01-01");
        order.setStatus("CANCELLED"); order.setFaultCategory("LEGACY"); order.setPriority("NORMAL");
        when(orders.selectExportList(query, reporter)).thenReturn(java.util.Collections.nCopies(1000, order));
        PageHelper.startPage(3, 1);
        var rows = service.export(query);
        assertEquals(1000, rows.size());
        assertNull(query.getReporterId());
        assertNotNull(query.getBeginTime());
        assertNull(PageHelper.getLocalPage());
        assertEquals("LEGACY", rows.get(0).getFaultCategory());
        assertEquals("CANCELLED", rows.get(0).getStatus());
        verify(orders).selectExportList(query, reporter);
        verify(orders, never()).selectList(any(), any());
    }

    @Test
    void exportRefuses1001RatherThanSilentlyTruncating()
    {
        when(orders.selectExportList(any(), eq(reporter))).thenReturn(java.util.Collections.nCopies(1001, order));
        error(400, () -> service.export(new WorkOrderQuery()));
    }

    @Test
    void exportedWorkbookHasChineseLabelsTimesAndNoInternalFields() throws Exception
    {
        when(dictionaries.selectDictDataByType(anyString())).thenAnswer(call -> {
            SysDictData entry = new SysDictData();
            String type = call.getArgument(0);
            entry.setDictValue("eq_priority".equals(type) ? "NORMAL" : "eq_work_order_status".equals(type) ? "CLOSED" : "MECHANICAL");
            entry.setDictLabel("eq_priority".equals(type) ? "普通" : "eq_work_order_status".equals(type) ? "已关闭" : "机械");
            return List.of(entry);
        });
        order.setTitle("=1+1"); order.setStatus("CLOSED"); order.setFaultCategory("MECHANICAL"); order.setPriority("NORMAL");
        order.setCreateTime(new java.util.Date()); order.setClosedTime(new java.util.Date());
        order.setSymptom("模拟异响"); order.setEquipmentCode("TEMP-EQ"); order.setReporterName("模拟报修");
        when(orders.selectExportList(any(), eq(reporter))).thenReturn(List.of(order));
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        new com.ruoyi.common.utils.poi.ExcelUtil<>(com.ruoyi.equipment.vo.WorkOrderExportRow.class)
                .exportExcel(response, service.export(new WorkOrderQuery()), "故障工单");
        try (var book = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(response.getContentAsByteArray())))
        {
            var sheet = book.getSheetAt(0);
            assertEquals(1, sheet.getLastRowNum());
            assertEquals(16, sheet.getRow(0).getLastCellNum());
            assertEquals("工单编号", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("机械", sheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals("普通", sheet.getRow(1).getCell(5).getStringCellValue());
            assertEquals("已关闭", sheet.getRow(1).getCell(9).getStringCellValue());
            assertFalse(sheet.getRow(1).getCell(11).getStringCellValue().isBlank());
            assertEquals(org.apache.poi.ss.usermodel.CellType.STRING, sheet.getRow(1).getCell(1).getCellType());
        }
    }

    private WorkOrderAssignRequest assignRequest()
    {
        WorkOrderAssignRequest request = new WorkOrderAssignRequest();
        request.setHandlerId(300L); request.setVersion(0);
        return request;
    }

    private WorkOrderCancelRequest cancelRequest()
    {
        WorkOrderCancelRequest request = new WorkOrderCancelRequest();
        request.setVersion(0); request.setReason("模拟取消");
        return request;
    }

    private void error(int code, org.junit.jupiter.api.function.Executable action)
    {
        assertEquals(code, assertThrows(ServiceException.class, action).getCode());
    }
}
