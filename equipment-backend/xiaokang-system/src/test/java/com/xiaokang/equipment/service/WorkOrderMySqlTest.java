package com.xiaokang.equipment.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import com.xiaokang.common.core.domain.entity.SysDictData;
import com.xiaokang.common.exception.ServiceException;
import com.xiaokang.equipment.dto.WorkOrderAssignRequest;
import com.xiaokang.equipment.dto.WorkOrderCancelRequest;
import com.xiaokang.equipment.dto.WorkOrderCreateRequest;
import com.xiaokang.equipment.dto.WorkOrderQuery;
import com.xiaokang.equipment.dto.WorkOrderProcessRequest;
import com.xiaokang.equipment.dto.WorkOrderSubmitReviewRequest;
import com.xiaokang.equipment.dto.WorkOrderReviewRequest;
import com.xiaokang.equipment.dto.StatisticsQuery;
import com.xiaokang.equipment.mapper.StatisticsMapper;
import com.xiaokang.equipment.mapper.EquipmentMapper;
import com.xiaokang.equipment.mapper.WorkOrderMapper;
import com.xiaokang.equipment.security.EquipmentActor;
import com.xiaokang.equipment.security.EquipmentAuthorization;
import com.xiaokang.equipment.service.impl.WorkOrderServiceImpl;
import com.xiaokang.equipment.vo.MaintainerOption;
import com.xiaokang.system.service.ISysDictTypeService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 真实MySQL事务与Mapper测试。仅在显式配置连接时运行。
 * 所有业务写入表在专用连接上创建同名TEMPORARY克隆，连接关闭即消失；账号表只读。
 * 单连接测试不代替跨连接并发锁测试。
 */
@EnabledIfEnvironmentVariable(named = "EQ_MYSQL_TEST_URL", matches = ".+")
class WorkOrderMySqlTest
{
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbc;
    private EquipmentAuthorization authorization;
    private IWorkOrderService service;
    private WorkOrderMapper mapper;
    private StatisticsService statistics;
    private final EquipmentActor reporter = new EquipmentActor(100L, "test_reporter", "模拟报修", false, true, false);
    private final EquipmentActor manager = new EquipmentActor(1L, "test_manager", "模拟管理", true, false, false);

    @BeforeEach
    void setup() throws Exception
    {
        Connection connection = DriverManager.getConnection(System.getenv("EQ_MYSQL_TEST_URL"),
                System.getenv("EQ_MYSQL_TEST_USER"), System.getenv("EQ_MYSQL_TEST_PASSWORD"));
        dataSource = new SingleConnectionDataSource(connection, true);
        jdbc = new JdbcTemplate(dataSource);
        // LIKE只复制结构，不复制任何生产/开发数据。后续表名均被临时表遮蔽。
        for (String table : List.of("eq_equipment", "eq_work_order", "eq_work_order_record"))
        {
            // MySQL不允许CREATE TEMPORARY TABLE x LIKE x，先克隆到另一个临时模板名。
            String template = "p3_template_" + table;
            jdbc.execute("CREATE TEMPORARY TABLE " + template + " LIKE " + table);
            jdbc.execute("CREATE TEMPORARY TABLE " + table + " LIKE " + template);
        }
        jdbc.update("INSERT INTO eq_equipment(equipment_id,equipment_code,equipment_name,equipment_type,location,equipment_status,version,create_by,create_time) VALUES(7,'TEMP-EQ','temporary','MOTOR','temporary','IN_SERVICE',0,'test',NOW())");
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new ClassPathResource("mapper/equipment/EquipmentMapper.xml"),
                new ClassPathResource("mapper/equipment/WorkOrderMapper.xml"),
                new ClassPathResource("mapper/equipment/StatisticsMapper.xml"));
        SqlSessionTemplate session = new SqlSessionTemplate(factory.getObject());
        mapper = spy(session.getMapper(WorkOrderMapper.class));
        // 账号表只读；MySQL临时表不能被同一查询以两个别名重复引用，保留真实用户关联查询。
        // 维护目标资格在单元和实际API验证，本组只为事务测试提供虚拟目标，绝不修改真实账号。
        MaintainerOption handler = new MaintainerOption();
        handler.setUserId(300L); handler.setUserName("test_maintainer"); handler.setNickName("Maintainer");
        doReturn(handler).when(mapper).lockMaintainer(300L);
        authorization = mock(EquipmentAuthorization.class);
        when(authorization.requireReporter()).thenReturn(reporter);
        when(authorization.requireReader()).thenReturn(reporter);
        when(authorization.requireManager()).thenReturn(manager);
        ISysDictTypeService dictionaries = mock(ISysDictTypeService.class);
        when(dictionaries.selectDictDataByType(anyString())).thenAnswer(call -> {
            SysDictData entry = new SysDictData();
            entry.setDictValue("eq_priority".equals(call.getArgument(0)) ? "NORMAL" : "MECHANICAL");
            entry.setStatus("0");
            return List.of(entry);
        });
        WorkOrderServiceImpl target = new WorkOrderServiceImpl(mapper, session.getMapper(EquipmentMapper.class), authorization, dictionaries);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource), new AnnotationTransactionAttributeSource()));
        service = (IWorkOrderService) proxy.getProxy();
        StatisticsMapper actualStatistics = session.getMapper(StatisticsMapper.class);
        StatisticsMapper statisticsMapper = spy(actualStatistics);
        doAnswer(call -> {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, connection.getTransactionIsolation());
            return actualStatistics.selectSummary(call.getArgument(0));
        }).when(statisticsMapper).selectSummary(any());
        ProxyFactory statisticsProxy = new ProxyFactory(new StatisticsService(statisticsMapper, authorization, dictionaries));
        statisticsProxy.setProxyTargetClass(true);
        statisticsProxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource), new AnnotationTransactionAttributeSource()));
        statistics = (StatisticsService) statisticsProxy.getProxy();
    }

    @AfterEach
    void cleanup()
    {
        if (dataSource != null) dataSource.destroy();
    }

    @Test
    void realMapperScopesListRecordsAndAssignment()
    {
        var created = service.create(request());
        assertEquals("WAITING", created.getStatus());
        assertEquals(1, service.records(created.getWorkOrderId()).size());
        WorkOrderAssignRequest assignment = new WorkOrderAssignRequest();
        assignment.setVersion(0); assignment.setHandlerId(300L);
        var assigned = service.assign(created.getWorkOrderId(), assignment);
        assertEquals(1, assigned.getVersion());
        assertEquals("PROCESSING", assigned.getStatus());
        assertEquals(2, mapper.selectRecords(created.getWorkOrderId()).size());
        assertEquals(300L, assigned.getHandlerId());
        when(authorization.requireReader()).thenReturn(new EquipmentActor(200L,"other","Other",false,true,false));
        assertTrue(service.list(new WorkOrderQuery(),1,10).isEmpty());
        assertEquals(404, assertThrows(ServiceException.class, () -> service.records(created.getWorkOrderId())).getCode());
        when(authorization.requireReader()).thenReturn(new EquipmentActor(300L,"maintainer","Maintainer",false,false,true));
        assertEquals(1, service.list(new WorkOrderQuery(),1,10).size());
        when(authorization.requireReporter()).thenReturn(new EquipmentActor(200L,"other","Other",false,true,false));
        var second = service.create(request());
        MaintainerOption dualRoleHandler = new MaintainerOption();
        dualRoleHandler.setUserId(100L); dualRoleHandler.setUserName("dual"); dualRoleHandler.setNickName("Dual");
        doReturn(dualRoleHandler).when(mapper).lockMaintainer(100L);
        assignment.setHandlerId(100L);
        service.assign(second.getWorkOrderId(), assignment);
        when(authorization.requireReader()).thenReturn(new EquipmentActor(100L,"dual","Dual",false,true,true));
        assertEquals(2, service.list(new WorkOrderQuery(),1,10).size());
        assertEquals(2, service.records(second.getWorkOrderId()).size());
    }

    @Test
    void recordFailureRollsBackCreatedOrder()
    {
        jdbc.execute("ALTER TABLE eq_work_order_record ADD CONSTRAINT temp_reject_create CHECK (action_type != 'CREATE')");
        assertThrows(DataAccessException.class, () -> service.create(request()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM eq_work_order", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM eq_work_order_record", Integer.class));
    }

    @Test
    void recordFailureRollsBackAssignmentAndCancellation()
    {
        var created = service.create(request());
        jdbc.execute("ALTER TABLE eq_work_order_record ADD CONSTRAINT temp_reject_action CHECK (action_type = 'CREATE')");
        WorkOrderAssignRequest assignment = new WorkOrderAssignRequest();
        assignment.setHandlerId(300L); assignment.setVersion(0);
        assertThrows(DataAccessException.class, () -> service.assign(created.getWorkOrderId(), assignment));
        assertEquals("WAITING", mapper.selectById(created.getWorkOrderId()).getStatus());
        assertEquals(0, mapper.selectById(created.getWorkOrderId()).getVersion());
        WorkOrderCancelRequest cancellation = new WorkOrderCancelRequest();
        cancellation.setVersion(0); cancellation.setReason("模拟取消");
        assertThrows(DataAccessException.class, () -> service.cancel(created.getWorkOrderId(), cancellation));
        assertEquals("WAITING", mapper.selectById(created.getWorkOrderId()).getStatus());
        assertEquals(1, mapper.selectRecords(created.getWorkOrderId()).size());
    }

    @Test
    void disabledTargetAndStaleVersionDoNotAppendRecords()
    {
        var created = service.create(request());
        doReturn(null).when(mapper).lockMaintainer(300L);
        WorkOrderAssignRequest assignment = new WorkOrderAssignRequest();
        assignment.setVersion(0); assignment.setHandlerId(300L);
        assertEquals(400, assertThrows(ServiceException.class, () -> service.assign(created.getWorkOrderId(),assignment)).getCode());
        WorkOrderCancelRequest cancellation = new WorkOrderCancelRequest();
        cancellation.setVersion(0); cancellation.setReason("模拟取消");
        assertEquals("CANCELLED", service.cancel(created.getWorkOrderId(), cancellation).getStatus());
        assertEquals(409, assertThrows(ServiceException.class, () -> service.cancel(created.getWorkOrderId(),cancellation)).getCode());
        assertEquals(2, mapper.selectRecords(created.getWorkOrderId()).size());
    }

    @Test
    void completeWorkflowRetainsEveryRoundAndOnlyApprovalCloses()
    {
        Long id = assignedOrder();
        var note = new WorkOrderProcessRequest(); note.setVersion(1); note.setDescription("第一轮模拟排查");
        assertEquals(2, service.process(id, note).getVersion());
        var first = submission(2, "第一轮模拟结果");
        var pending = service.submitReview(id, first);
        assertEquals("PENDING_REVIEW", pending.getStatus());
        assertNotNull(pending.getLastSubmitTime());
        assertNull(pending.getClosedTime());
        var reject = review(3, "REJECT");
        var returned = service.review(id, reject);
        assertEquals("PROCESSING", returned.getStatus());
        assertEquals(300L, returned.getHandlerId());
        assertNull(returned.getClosedTime());
        service.submitReview(id, submission(4, "第二轮模拟结果"));
        var closed = service.review(id, review(5, "APPROVE"));
        assertEquals("CLOSED", closed.getStatus());
        assertEquals(6, closed.getVersion());
        assertNotNull(closed.getClosedTime());
        var history = mapper.selectRecords(id);
        assertEquals(List.of("CREATE", "ASSIGN", "PROCESS_NOTE", "SUBMIT_REVIEW", "REVIEW_REJECT", "SUBMIT_REVIEW", "REVIEW_APPROVE"),
                history.stream().map(r -> r.getActionType()).toList());
        assertEquals("第一轮模拟结果", history.get(3).getResultDescription());
        assertEquals("第二轮模拟结果", history.get(5).getResultDescription());
        assertEquals("模拟措施", history.get(3).getMeasures());
        assertEquals("待确认", history.get(5).getCauseAnalysis());
        for (int i = 1; i < history.size(); i++)
        {
            assertTrue(history.get(i).getRecordId() > history.get(i - 1).getRecordId());
        }
        assertEquals("IN_SERVICE", jdbc.queryForObject("SELECT equipment_status FROM eq_equipment WHERE equipment_id=7", String.class));
        note.setVersion(6);
        assertEquals(400, assertThrows(ServiceException.class, () -> service.process(id, note)).getCode());
        assertEquals(7, mapper.selectRecords(id).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROCESS_NOTE", "SUBMIT_REVIEW", "REVIEW_REJECT", "REVIEW_APPROVE"})
    void recordFailureRollsBackEachWorkflowAction(String action)
    {
        Long id = assignedOrder();
        boolean reviewing = action.startsWith("REVIEW_");
        if (reviewing) service.submitReview(id, submission(1, "模拟待复核结果"));
        var before = mapper.selectById(id);
        int count = mapper.selectRecords(id).size();
        jdbc.execute("ALTER TABLE eq_work_order_record ADD CONSTRAINT temp_reject_p4 CHECK (action_type != '" + action + "')");
        assertThrows(DataAccessException.class, () -> {
            if ("PROCESS_NOTE".equals(action))
            {
                var note = new WorkOrderProcessRequest(); note.setVersion(1); note.setDescription("模拟失败追加");
                service.process(id, note);
            }
            else if ("SUBMIT_REVIEW".equals(action)) service.submitReview(id, submission(1, "模拟失败提交"));
            else service.review(id, review(2, "REVIEW_APPROVE".equals(action) ? "APPROVE" : "REJECT"));
        });
        var after = mapper.selectById(id);
        assertEquals(before.getStatus(), after.getStatus());
        assertEquals(before.getVersion(), after.getVersion());
        assertEquals(before.getLastSubmitTime(), after.getLastSubmitTime());
        assertEquals(before.getClosedTime(), after.getClosedTime());
        assertEquals(before.getHandlerId(), after.getHandlerId());
        assertEquals(count, mapper.selectRecords(id).size());
    }

    @Test
    void staleWorkflowActionDoesNotAppendOrOverwriteResult()
    {
        Long id = assignedOrder();
        service.submitReview(id, submission(1, "唯一有效结果"));
        assertEquals(409, assertThrows(ServiceException.class,
                () -> service.submitReview(id, submission(1, "过期结果"))).getCode());
        service.review(id, review(2, "APPROVE"));
        assertEquals(409, assertThrows(ServiceException.class,
                () -> service.review(id, review(2, "REJECT"))).getCode());
        assertEquals(4, mapper.selectRecords(id).size());
        assertEquals("唯一有效结果", mapper.selectRecords(id).get(2).getResultDescription());
        assertEquals("CLOSED", mapper.selectById(id).getStatus());
    }

    private Long assignedOrder()
    {
        var created = service.create(request());
        var assignment = new WorkOrderAssignRequest(); assignment.setVersion(0); assignment.setHandlerId(300L);
        service.assign(created.getWorkOrderId(), assignment);
        when(authorization.requireReader()).thenReturn(new EquipmentActor(300L,"maintainer","模拟维护",false,false,true));
        return created.getWorkOrderId();
    }

    private WorkOrderSubmitReviewRequest submission(int version, String result)
    {
        var request = new WorkOrderSubmitReviewRequest(); request.setVersion(version);
        request.setCauseAnalysis("待确认"); request.setMeasures("模拟措施"); request.setResultDescription(result); return request;
    }

    private WorkOrderReviewRequest review(int version, String decision)
    {
        var request = new WorkOrderReviewRequest(); request.setVersion(version);
        request.setDecision(decision); request.setDescription("模拟复核说明"); return request;
    }

    @Test
    void statisticsRespectDateBoundaryCancellationAndGlobalEquipmentCounts()
    {
        seedStatistics();
        var query = day("2026-01-01");
        var overview = statistics.overview(query);
        assertEquals(3, overview.getEquipmentTotal());
        assertEquals(6, overview.getOrderTotal());
        assertEquals(3, overview.getOpenOrderCount());
        assertEquals(2, overview.getClosedOrderCount());
        assertEquals(0, new java.math.BigDecimal("1.50").compareTo(overview.getAverageClosedHours()));
        assertEquals(6, overview.getOrderStatuses().stream().mapToLong(r -> r.getCount()).sum());
        assertEquals(5, overview.getFaultCategories().stream().mapToLong(r -> r.getCount()).sum());
        assertEquals(3, overview.getFaultCategories().stream().filter(r -> "MECHANICAL".equals(r.getCode())).findFirst().orElseThrow().getCount());
        assertEquals(7L, overview.getTopEquipment().get(0).getEquipmentId());
        assertEquals(3, overview.getTopEquipment().get(0).getCount());
        query.setEquipmentId(8L);
        overview = statistics.overview(query);
        assertEquals(3, overview.getEquipmentTotal());
        assertEquals(2, overview.getOrderTotal());
        assertEquals(1, overview.getClosedOrderCount());
        assertEquals(0, java.math.BigDecimal.ONE.compareTo(overview.getAverageClosedHours()));
        var empty = statistics.overview(day("2027-01-01"));
        assertEquals(0, empty.getOrderTotal()); assertEquals(3, empty.getEquipmentTotal());
        assertNull(empty.getAverageClosedHours());
        assertEquals(5, empty.getOrderStatuses().size());
    }

    @Test
    void rankingUsesTopTenCodeTieBreakAndExcludesCancelledOnlyDevices()
    {
        for (int i = 0; i < 13; i++)
        {
            long id = 100 + i;
            jdbc.update("INSERT INTO eq_equipment(equipment_id,equipment_code,equipment_name,equipment_type,location,equipment_status,version,create_by,create_time) VALUES(?,?,?,'OTHER','temporary','IN_SERVICE',0,'test',NOW())", id, String.format("RANK-%02d",i), "temporary rank");
            seedOrder(id, id, i == 0 ? "CANCELLED" : "WAITING", "MECHANICAL", "2026-01-01 09:00:00", null,100,300);
        }
        var result = statistics.overview(day("2026-01-01"));
        assertEquals(13, result.getOrderTotal());
        assertEquals(10, result.getTopEquipment().size());
        assertEquals("RANK-01", result.getTopEquipment().get(0).getEquipmentCode());
        assertEquals("RANK-10", result.getTopEquipment().get(9).getEquipmentCode());
        assertNull(result.getAverageClosedHours());
    }

    @Test
    void exportedRowsMatchListFiltersAndRoleUnionInRealSql()
    {
        seedStatistics();
        var query = new WorkOrderQuery(); query.setBeginDate("2026-01-01"); query.setEndDate("2026-01-01"); query.setStatus("CLOSED");
        var exported = service.export(query);
        assertEquals(List.of("P5-204"), exported.stream().map(r -> r.getWorkOrderNo()).toList());
        assertEquals(service.list(query,1,100).size(), exported.size());
        jdbc.update("UPDATE eq_work_order SET handler_id=100 WHERE work_order_id=205");
        when(authorization.requireReader()).thenReturn(new EquipmentActor(100L,"dual","Dual",false,true,true));
        exported = service.export(query);
        assertEquals(2, exported.size());
        assertTrue(exported.stream().anyMatch(row -> "P5-205".equals(row.getWorkOrderNo())));
        when(authorization.requireReader()).thenReturn(manager);
        exported = service.export(query);
        assertEquals(2, exported.size());
        query.setEquipmentId(7L);
        assertEquals(1, service.export(query).size());
    }

    @Test
    void realExportLimitFetches1001AndAcceptsExactly1000()
    {
        List<Object[]> batch = new java.util.ArrayList<>();
        for (int i = 1; i <= 1001; i++) batch.add(new Object[] {"P5-LIMIT-" + i});
        jdbc.batchUpdate("INSERT INTO eq_work_order(work_order_no,equipment_id,title,symptom,fault_category,priority,occurred_time,reporter_id,status,version,create_by,create_time) VALUES(?,7,'temporary','temporary','MECHANICAL','NORMAL','2026-01-01 09:00:00',100,'WAITING',0,'test','2026-01-01 09:00:00')", batch);
        assertEquals(1001, mapper.selectExportList(new WorkOrderQuery(),reporter).size());
        assertEquals(400, assertThrows(ServiceException.class, () -> service.export(new WorkOrderQuery())).getCode());
        jdbc.update("DELETE FROM eq_work_order WHERE work_order_no='P5-LIMIT-1001'");
        assertEquals(1000, service.export(new WorkOrderQuery()).size());
    }

    private StatisticsQuery day(String date)
    {
        var query = new StatisticsQuery(); query.setBeginDate(date); query.setEndDate(date); return query;
    }

    private void seedStatistics()
    {
        jdbc.update("INSERT INTO eq_equipment(equipment_id,equipment_code,equipment_name,equipment_type,location,equipment_status,version,create_by,create_time) VALUES(8,'TEMP-EQ-8','temporary','OTHER','temporary','OUT_OF_SERVICE',0,'test',NOW()),(9,'TEMP-EQ-9','temporary','OTHER','temporary','RETIRED',0,'test',NOW())");
        seedOrder(201,7,"WAITING","MECHANICAL","2026-01-01 00:00:00",null,100,300);
        seedOrder(202,7,"PROCESSING","ELECTRICAL","2026-01-01 12:00:00",null,100,300);
        seedOrder(203,8,"PENDING_REVIEW","MECHANICAL","2026-01-01 13:00:00",null,200,400);
        seedOrder(204,7,"CLOSED","MECHANICAL","2026-01-01 14:00:00","2026-01-01 16:00:00",100,300);
        seedOrder(205,8,"CLOSED","ELECTRICAL","2026-01-01 22:00:00","2026-01-01 23:00:00",200,400);
        seedOrder(206,7,"CANCELLED","SOFTWARE","2026-01-01 23:59:59",null,100,300);
        seedOrder(207,7,"WAITING","MECHANICAL","2025-12-31 23:59:59",null,100,300);
        seedOrder(208,7,"CLOSED","MECHANICAL","2026-01-02 00:00:00","2026-01-02 01:00:00",100,300);
    }

    private void seedOrder(long id, long equipmentId, String status, String category, String created, String closed, long reporterId, long handlerId)
    {
        jdbc.update("INSERT INTO eq_work_order(work_order_id,work_order_no,equipment_id,title,symptom,fault_category,priority,occurred_time,reporter_id,handler_id,status,version,create_by,create_time,closed_time) VALUES(?,?,?,'temporary','temporary',?,'NORMAL',?,?,?,?,0,'test',?,?)",
                id,"P5-"+id,equipmentId,category,created,reporterId,handlerId,status,created,closed);
    }

    private WorkOrderCreateRequest request()
    {
        WorkOrderCreateRequest request = new WorkOrderCreateRequest();
        request.setEquipmentId(7L); request.setTitle("临时连接模拟故障"); request.setSymptom("temporary test data");
        request.setOccurredTime("2020-01-01 00:00:00"); request.setFaultCategory("MECHANICAL"); request.setPriority("NORMAL");
        return request;
    }
}
