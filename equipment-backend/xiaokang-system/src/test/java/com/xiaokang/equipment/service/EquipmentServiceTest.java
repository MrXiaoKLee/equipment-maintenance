package com.xiaokang.equipment.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.github.pagehelper.PageHelper;
import com.xiaokang.common.core.domain.entity.SysDictData;
import com.xiaokang.common.core.domain.entity.SysRole;
import com.xiaokang.common.core.domain.entity.SysUser;
import com.xiaokang.common.core.domain.model.LoginUser;
import com.xiaokang.common.exception.ServiceException;
import com.xiaokang.equipment.domain.Equipment;
import com.xiaokang.equipment.dto.EquipmentCreateRequest;
import com.xiaokang.equipment.dto.EquipmentEditRequest;
import com.xiaokang.equipment.dto.EquipmentQuery;
import com.xiaokang.equipment.dto.EquipmentStatusRequest;
import com.xiaokang.equipment.mapper.EquipmentMapper;
import com.xiaokang.equipment.service.impl.EquipmentServiceImpl;
import com.xiaokang.system.service.ISysDictTypeService;
import com.xiaokang.system.service.ISysUserService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 不连接数据库，验证业务约束及权限拒绝时没有写入。 */
class EquipmentServiceTest
{
    private EquipmentMapper mapper;
    private ISysDictTypeService dictionaries;
    private ISysUserService users;
    private EquipmentServiceImpl service;

    @BeforeEach
    void setup()
    {
        mapper = mock(EquipmentMapper.class);
        dictionaries = mock(ISysDictTypeService.class);
        users = mock(ISysUserService.class);
        service = new EquipmentServiceImpl(mapper, dictionaries, new com.xiaokang.equipment.security.EquipmentAuthorization(users));
        login(1L);
    }

    @AfterEach
    void cleanup()
    {
        SecurityContextHolder.clearContext();
        PageHelper.clearPage();
    }

    @Test
    void createsNormalizedEquipmentWithServerOwnedState()
    {
        activeType();
        Equipment created = service.create(createRequest());
        assertEquals("EQ-TEST", created.getEquipmentCode());
        assertEquals("模拟电机", created.getEquipmentName());
        assertEquals("IN_SERVICE", created.getEquipmentStatus());
        assertEquals(0, created.getVersion());
        assertEquals("test-user", created.getCreateBy());
        assertNotNull(created.getCreateTime());
        verify(mapper).insert(created);
    }

    @Test
    void rejectsExistingNormalizedCode()
    {
        when(mapper.selectByCode("EQ-TEST")).thenReturn(equipment());
        assertError(400, () -> service.create(createRequest()));
        verify(mapper, never()).insert(any());
    }

    @Test
    void handlesConcurrentUniqueConstraintViolation()
    {
        activeType();
        when(mapper.insert(any())).thenThrow(new DuplicateKeyException("unique"));
        ServiceException error = assertError(400, () -> service.create(createRequest()));
        assertEquals("设备编号已存在", error.getMessage());
    }

    @Test
    void rejectsInvalidCodeAndBlankRequiredField()
    {
        EquipmentCreateRequest request = createRequest();
        request.setEquipmentCode("中文编号");
        assertError(400, () -> service.create(request));
        request.setEquipmentCode("EQ-TEST");
        request.setLocation("  ");
        assertError(400, () -> service.create(request));
        verify(mapper, never()).insert(any());
    }

    @Test
    void rejectsOverlongRemark()
    {
        EquipmentCreateRequest request = createRequest();
        request.setRemark("x".repeat(2001));
        assertError(400, () -> service.create(request));
        verify(mapper, never()).insert(any());
    }

    @Test
    void rejectsUnavailableType()
    {
        when(dictionaries.selectDictDataByType("eq_equipment_type")).thenReturn(List.of());
        assertError(400, () -> service.create(createRequest()));
        verify(mapper, never()).insert(any());
    }

    @Test
    void missingDictionaryIsAValidationError()
    {
        when(dictionaries.selectDictDataByType("eq_equipment_type")).thenReturn(null);
        assertError(400, () -> service.create(createRequest()));
    }

    @Test
    void disabledAccountAndRevokedRoleCannotUseCachedPermissions()
    {
        login(100L, "eq_manager");
        SysUser current = users.selectUserById(100L);
        current.setStatus("1");
        assertError(403, () -> service.create(createRequest()));
        current.setStatus("0");
        current.setRoles(List.of());
        assertError(403, () -> service.create(createRequest()));
        verify(mapper, never()).insert(any());
    }

    @Test
    void paginationStartsAfterAuthorizationAndIsClearedOnFailure()
    {
        when(users.selectUserById(1L)).thenAnswer(invocation -> {
            assertNull(PageHelper.getLocalPage());
            SysUser user = new SysUser();
            user.setStatus("0");
            user.setDelFlag("0");
            return user;
        });
        when(mapper.selectList(any())).thenAnswer(invocation -> {
            assertEquals(100, PageHelper.getLocalPage().getPageSize());
            throw new IllegalStateException("模拟查询失败");
        });
        assertThrows(IllegalStateException.class, () -> service.list(new EquipmentQuery(), 1, 1000));
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void readerCannotCreateEditChangeStateOrExport()
    {
        login(100L, "eq_reporter");
        assertError(403, () -> service.create(createRequest()));
        assertError(403, () -> service.edit(7L, editRequest(0)));
        assertError(403, () -> service.changeStatus(7L, status("RETIRED", 0)));
        assertError(403, () -> service.export(new EquipmentQuery()));
        verifyNoInteractions(mapper);
    }

    @Test
    void nonBusinessRoleCannotRead()
    {
        login(100L, "common");
        assertError(403, () -> service.get(7L));
        verifyNoInteractions(mapper);
    }

    @Test
    void editsAllowedFieldsAndPreservesCodeAndState()
    {
        Equipment original = equipment();
        when(mapper.lockById(7L)).thenReturn(original);
        when(mapper.update(any())).thenReturn(1);
        Equipment updated = service.edit(7L, editRequest(0));
        assertEquals("EQ-TEST", updated.getEquipmentCode());
        assertEquals("IN_SERVICE", updated.getEquipmentStatus());
        assertEquals("更名电机", updated.getEquipmentName());
        assertEquals(1, updated.getVersion());
        verifyNoInteractions(dictionaries);
    }

    @Test
    void rejectsStaleVersionBeforeEditing()
    {
        Equipment original = equipment();
        original.setVersion(2);
        when(mapper.lockById(7L)).thenReturn(original);
        assertError(409, () -> service.edit(7L, editRequest(1)));
        verify(mapper, never()).update(any());
    }

    @Test
    void rejectsMissingVersionAndMissingDevice()
    {
        when(mapper.lockById(7L)).thenReturn(equipment());
        assertError(400, () -> service.edit(7L, editRequest(null)));
        assertError(404, () -> service.get(999L));
    }

    @Test
    void refusesRetirementWithOpenOrders()
    {
        when(mapper.lockById(7L)).thenReturn(equipment());
        when(mapper.countOpenWorkOrders(7L)).thenReturn(1);
        assertError(400, () -> service.changeStatus(7L, status("RETIRED", 0)));
        verify(mapper, never()).updateStatus(any());
    }

    @Test
    void allowsDisableWithOpenOrdersAndRetirementWithoutOpenOrders()
    {
        when(mapper.lockById(7L)).thenAnswer(invocation -> equipment());
        when(mapper.updateStatus(any())).thenReturn(1);
        assertEquals("OUT_OF_SERVICE", service.changeStatus(7L, status("OUT_OF_SERVICE", 0)).getEquipmentStatus());
        verify(mapper, never()).countOpenWorkOrders(any());
        assertEquals("RETIRED", service.changeStatus(7L, status("RETIRED", 0)).getEquipmentStatus());
    }

    @Test
    void retiredStateIsTerminal()
    {
        Equipment original = equipment();
        original.setEquipmentStatus("RETIRED");
        when(mapper.lockById(7L)).thenReturn(original);
        assertError(400, () -> service.changeStatus(7L, status("IN_SERVICE", 0)));
        assertError(400, () -> service.changeStatus(7L, status("RETIRED", 0)));
        verify(mapper, never()).updateStatus(any());
    }

    @Test
    void historyUsesUnionOfCurrentUsersRolesAndClearsPagination()
    {
        login(100L, "eq_reporter", "eq_maintainer");
        when(mapper.selectById(7L)).thenAnswer(invocation -> {
            assertNull(PageHelper.getLocalPage());
            return equipment();
        });
        when(mapper.selectHistory(7L, 100L, false, true, true)).thenAnswer(invocation -> {
            assertEquals(100, PageHelper.getLocalPage().getPageSize());
            return List.of();
        });
        assertTrue(service.history(7L, 1, 1000).isEmpty());
        assertNull(PageHelper.getLocalPage());
        verify(mapper).selectHistory(7L, 100L, false, true, true);
    }

    @Test
    void exportRejectsOverLimitInsteadOfTruncating()
    {
        when(mapper.selectExportList(any())).thenReturn(Collections.nCopies(1001, equipment()));
        assertError(400, () -> service.export(new EquipmentQuery()));
    }

    @Test
    void exportPreservesLegacyTypeCodeWhenDictionaryEntryIsUnavailable()
    {
        Equipment row = equipment();
        row.setEquipmentType("LEGACY");
        when(mapper.selectExportList(any())).thenReturn(List.of(row));
        when(dictionaries.selectDictDataByType("eq_equipment_type")).thenReturn(null);
        assertEquals("LEGACY", service.export(new EquipmentQuery()).get(0).getEquipmentTypeLabel());
    }

    private void login(Long id, String... roleKeys)
    {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setUserName("test-user");
        user.setStatus("0");
        user.setDelFlag("0");
        user.setRoles(java.util.Arrays.stream(roleKeys).map(key -> {
            SysRole role = new SysRole();
            role.setRoleKey(key);
            role.setStatus("0");
            role.setDelFlag("0");
            return role;
        }).toList());
        LoginUser principal = new LoginUser(id, 100L, user, Set.of());
        when(users.selectUserById(id)).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private void activeType()
    {
        SysDictData type = new SysDictData();
        type.setDictValue("MOTOR");
        type.setStatus("0");
        when(dictionaries.selectDictDataByType("eq_equipment_type")).thenReturn(List.of(type));
    }

    private Equipment equipment()
    {
        Equipment equipment = new Equipment();
        equipment.setEquipmentId(7L);
        equipment.setEquipmentCode("EQ-TEST");
        equipment.setEquipmentType("MOTOR");
        equipment.setEquipmentStatus("IN_SERVICE");
        equipment.setVersion(0);
        return equipment;
    }

    private EquipmentCreateRequest createRequest()
    {
        EquipmentCreateRequest request = new EquipmentCreateRequest();
        request.setEquipmentCode(" eq-test ");
        request.setEquipmentName(" 模拟电机 ");
        request.setEquipmentType("MOTOR");
        request.setLocation(" 模拟实验室 ");
        return request;
    }

    private EquipmentEditRequest editRequest(Integer version)
    {
        EquipmentEditRequest request = new EquipmentEditRequest();
        request.setEquipmentName("更名电机");
        request.setEquipmentType("MOTOR");
        request.setLocation("模拟实验室");
        request.setVersion(version);
        return request;
    }

    private EquipmentStatusRequest status(String state, Integer version)
    {
        EquipmentStatusRequest request = new EquipmentStatusRequest();
        request.setEquipmentStatus(state);
        request.setVersion(version);
        return request;
    }

    private ServiceException assertError(int code, org.junit.jupiter.api.function.Executable action)
    {
        ServiceException error = assertThrows(ServiceException.class, action);
        assertEquals(code, error.getCode());
        return error;
    }
}
