package com.xiaokang.equipment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.xiaokang.common.exception.ServiceException;
import com.xiaokang.equipment.domain.WorkOrder;
import com.xiaokang.equipment.domain.WorkOrderRecord;
import com.xiaokang.equipment.dto.WorkOrderProcessRequest;
import com.xiaokang.equipment.dto.WorkOrderSubmitReviewRequest;
import com.xiaokang.equipment.dto.WorkOrderReviewRequest;
import com.xiaokang.equipment.mapper.EquipmentMapper;
import com.xiaokang.equipment.mapper.WorkOrderMapper;
import com.xiaokang.equipment.security.EquipmentActor;
import com.xiaokang.equipment.security.EquipmentAuthorization;
import com.xiaokang.equipment.service.impl.WorkOrderServiceImpl;
import com.xiaokang.system.service.ISysDictTypeService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkOrderWorkflowTest
{
    private WorkOrderMapper mapper;
    private EquipmentMapper equipment;
    private EquipmentAuthorization authorization;
    private WorkOrderServiceImpl service;
    private WorkOrder order;

    @BeforeEach
    void setup()
    {
        mapper = mock(WorkOrderMapper.class);
        equipment = mock(EquipmentMapper.class);
        authorization = mock(EquipmentAuthorization.class);
        service = new WorkOrderServiceImpl(mapper, equipment, authorization, mock(ISysDictTypeService.class));
        order = new WorkOrder();
        order.setWorkOrderId(9L); order.setEquipmentId(7L); order.setHandlerId(300L);
        order.setReporterId(100L); order.setStatus("PROCESSING"); order.setVersion(1);
        when(mapper.selectById(9L)).thenReturn(order);
        when(mapper.lockById(9L)).thenReturn(order);
        when(mapper.updateWorkflow(any(), anyString(), nullable(Long.class))).thenReturn(1);
        when(mapper.insertRecord(any())).thenReturn(1);
        when(authorization.requireReader()).thenReturn(new EquipmentActor(300L,"maintainer","模拟维护",false,false,true));
        when(authorization.requireManager()).thenReturn(new EquipmentActor(200L,"manager","模拟管理",true,false,false));
    }

    @Test
    void noteRemainsProcessingAndKeepsHandlerAndServerActor()
    {
        service.process(9L, note());
        assertEquals("PROCESSING", order.getStatus());
        assertEquals(300L, order.getHandlerId());
        assertNull(order.getClosedTime());
        var record = org.mockito.ArgumentCaptor.forClass(WorkOrderRecord.class);
        verify(mapper).insertRecord(record.capture());
        assertEquals("PROCESS_NOTE", record.getValue().getActionType());
        assertEquals("PROCESSING", record.getValue().getFromStatus());
        assertEquals("PROCESSING", record.getValue().getToStatus());
        assertEquals(300L, record.getValue().getActorId());
        assertEquals("模拟排查", record.getValue().getDescription());
        assertNotNull(record.getValue().getCreateTime());
        var sequence = inOrder(equipment, mapper);
        sequence.verify(equipment).lockById(7L);
        sequence.verify(mapper).lockById(9L);
        sequence.verify(mapper).updateWorkflow(order, "PROCESSING", 300L);
        sequence.verify(mapper).insertRecord(any());
    }

    @Test
    void submissionStoresSeparateTrimmedFieldsAndServerTime()
    {
        service.submitReview(9L, submission());
        assertEquals("PENDING_REVIEW", order.getStatus());
        assertNotNull(order.getLastSubmitTime());
        assertNull(order.getClosedTime());
        var record = org.mockito.ArgumentCaptor.forClass(WorkOrderRecord.class);
        verify(mapper).insertRecord(record.capture());
        assertEquals("SUBMIT_REVIEW", record.getValue().getActionType());
        assertEquals("待确认", record.getValue().getCauseAnalysis());
        assertEquals("模拟检查连接", record.getValue().getMeasures());
        assertEquals("模拟复查正常", record.getValue().getResultDescription());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVE", "REJECT"})
    void reviewPreservesHandlerAndOnlyApprovalSetsCloseTime(String decision)
    {
        order.setStatus("PENDING_REVIEW");
        var request = review(decision);
        service.review(9L, request);
        assertEquals("APPROVE".equals(decision) ? "CLOSED" : "PROCESSING", order.getStatus());
        assertEquals("APPROVE".equals(decision), order.getClosedTime() != null);
        assertEquals(300L, order.getHandlerId());
        var record = org.mockito.ArgumentCaptor.forClass(WorkOrderRecord.class);
        verify(mapper).insertRecord(record.capture());
        assertEquals("APPROVE".equals(decision) ? "REVIEW_APPROVE" : "REVIEW_REJECT", record.getValue().getActionType());
        assertEquals("PENDING_REVIEW", record.getValue().getFromStatus());
        assertEquals(200L, record.getValue().getActorId());
    }

    @Test
    void visibleReporterAndManagerCannotActAsHandler()
    {
        for (EquipmentActor actor : new EquipmentActor[] {
                new EquipmentActor(100L,"reporter","报修",false,true,false),
                new EquipmentActor(200L,"manager","管理",true,false,false),
                new EquipmentActor(1L,"admin","管理员",true,true,true),
                new EquipmentActor(300L,"former","已移除维护角色",true,false,false) })
        {
            when(authorization.requireReader()).thenReturn(actor);
            error(403, () -> service.process(9L, note()));
            error(403, () -> service.submitReview(9L, submission()));
        }
        assertNoWrite();
    }

    @Test
    void unassignedMaintainerCannotEnumerateOrChangeOrder()
    {
        when(authorization.requireReader()).thenReturn(new EquipmentActor(400L,"other","其他维护",false,false,true));
        error(404, () -> service.process(9L, note()));
        error(404, () -> service.submitReview(9L, submission()));
        assertNoWrite();
    }

    @Test
    void handlerWithoutManagerRoleCannotReview()
    {
        when(authorization.requireManager()).thenThrow(new ServiceException("只有设备管理人员",403));
        error(403, () -> service.review(9L, review("APPROVE")));
        assertNoWrite();
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "PENDING_REVIEW", "CLOSED", "CANCELLED"})
    void processingActionsRejectOtherStates(String status)
    {
        order.setStatus(status);
        error(400, () -> service.process(9L, note()));
        error(400, () -> service.submitReview(9L, submission()));
        assertNoWrite();
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "PROCESSING", "CLOSED", "CANCELLED"})
    void reviewOnlyAcceptsPendingState(String status)
    {
        order.setStatus(status);
        error(400, () -> service.review(9L, review("APPROVE")));
        assertNoWrite();
    }

    @Test
    void staleAndMissingVersionsRejectEveryWorkflowAction()
    {
        var note = note(); var submit = submission(); var review = review("APPROVE");
        for (Integer version : new Integer[] {null, -1, 0})
        {
            int expected = version == null || version < 0 ? 400 : 409;
            note.setVersion(version); submit.setVersion(version); review.setVersion(version);
            error(expected, () -> service.process(9L, note));
            error(expected, () -> service.submitReview(9L, submit));
            error(expected, () -> service.review(9L, review));
        }
        assertNoWrite();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cause", "measures", "result"})
    void eachSubmissionFieldIsRequiredAndLimited(String field)
    {
        for (String value : new String[] {null, "   ", "x".repeat(2001)})
        {
            var request = submission();
            if ("cause".equals(field)) request.setCauseAnalysis(value);
            if ("measures".equals(field)) request.setMeasures(value);
            if ("result".equals(field)) request.setResultDescription(value);
            error(400, () -> service.submitReview(9L, request));
        }
        assertNoWrite();
    }

    @Test
    void noteAndBothReviewDecisionsRequireNonemptyBoundedDescription()
    {
        for (String value : new String[] {null, "  ", "x".repeat(2001)})
        {
            var request = note(); request.setDescription(value);
            error(400, () -> service.process(9L, request));
            order.setStatus("PENDING_REVIEW");
            for (String decision : new String[] {"APPROVE", "REJECT"})
            {
                var review = review(decision); review.setDescription(value);
                error(400, () -> service.review(9L, review));
            }
            order.setStatus("PROCESSING");
        }
        assertNoWrite();
    }

    @Test
    void invalidDecisionAndFailedUpdateCannotCreateExtraRecord()
    {
        order.setStatus("PENDING_REVIEW");
        error(400, () -> service.review(9L, review("CLOSED")));
        when(mapper.updateWorkflow(any(), anyString(), nullable(Long.class))).thenReturn(0);
        error(409, () -> service.review(9L, review("APPROVE")));
        verify(mapper, never()).insertRecord(any());
    }

    private WorkOrderProcessRequest note()
    {
        var r = new WorkOrderProcessRequest(); r.setVersion(1); r.setDescription(" 模拟排查 "); return r;
    }

    private WorkOrderSubmitReviewRequest submission()
    {
        var r = new WorkOrderSubmitReviewRequest(); r.setVersion(1);
        r.setCauseAnalysis(" 待确认 "); r.setMeasures("模拟检查连接"); r.setResultDescription("模拟复查正常"); return r;
    }

    private WorkOrderReviewRequest review(String decision)
    {
        var r = new WorkOrderReviewRequest(); r.setVersion(1); r.setDecision(decision); r.setDescription("模拟复核说明"); return r;
    }

    private void assertNoWrite()
    {
        verify(mapper, never()).updateWorkflow(any(), anyString(), nullable(Long.class));
        verify(mapper, never()).insertRecord(any());
    }

    private void error(int code, org.junit.jupiter.api.function.Executable action)
    {
        assertEquals(code, assertThrows(ServiceException.class, action).getCode());
    }
}
