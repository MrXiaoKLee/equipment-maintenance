package com.ruoyi.equipment.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
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
                new ClassPathResource("mapper/equipment/WorkOrderMapper.xml"));
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

    private WorkOrderCreateRequest request()
    {
        WorkOrderCreateRequest request = new WorkOrderCreateRequest();
        request.setEquipmentId(7L); request.setTitle("临时连接模拟故障"); request.setSymptom("temporary test data");
        request.setOccurredTime("2020-01-01 00:00:00"); request.setFaultCategory("MECHANICAL"); request.setPriority("NORMAL");
        return request;
    }
}
