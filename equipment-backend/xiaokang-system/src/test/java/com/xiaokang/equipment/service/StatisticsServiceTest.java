package com.xiaokang.equipment.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.xiaokang.common.exception.ServiceException;
import com.xiaokang.equipment.dto.StatisticsQuery;
import com.xiaokang.equipment.mapper.StatisticsMapper;
import com.xiaokang.equipment.security.EquipmentAuthorization;
import com.xiaokang.equipment.vo.CountBucket;
import com.xiaokang.equipment.vo.StatisticsOverview;
import com.xiaokang.system.service.ISysDictTypeService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsServiceTest
{
    private StatisticsMapper mapper;
    private EquipmentAuthorization authorization;
    private StatisticsService service;

    @BeforeEach
    void setup()
    {
        mapper = mock(StatisticsMapper.class);
        authorization = mock(EquipmentAuthorization.class);
        service = new StatisticsService(mapper, authorization, mock(ISysDictTypeService.class));
        when(mapper.selectSummary(any())).thenReturn(new StatisticsOverview());
        when(mapper.selectEquipmentStatuses()).thenReturn(List.of());
        when(mapper.selectOrderStatuses(any())).thenReturn(List.of());
        when(mapper.selectFaultCategories(any())).thenReturn(List.of());
        when(mapper.selectTopEquipment(any())).thenReturn(List.of());
    }

    @Test
    void emptyResultsKeepNullAverageAndFillAllStatusBuckets()
    {
        var result = service.overview(new StatisticsQuery());
        assertEquals(0, result.getOrderTotal());
        assertEquals(0, result.getEquipmentTotal());
        assertNull(result.getAverageClosedHours());
        assertEquals(3, result.getEquipmentStatuses().size());
        assertEquals(5, result.getOrderStatuses().size());
        assertTrue(result.getOrderStatuses().stream().allMatch(bucket -> bucket.getCount() == 0));
        assertNotNull(result.getGeneratedTime());
    }

    @Test
    void unauthorizedActorCannotReadEvenWhenGivenQueryButtonPermission()
    {
        when(authorization.requireManager()).thenThrow(new ServiceException("只有设备管理人员",403));
        assertEquals(403, assertThrows(ServiceException.class, () -> service.overview(new StatisticsQuery())).getCode());
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "2026/01/01", "9999-01-01"})
    void invalidDatesAreRejectedBeforeAggregation(String date)
    {
        var query = new StatisticsQuery(); query.setEndDate(date);
        assertEquals(400, assertThrows(ServiceException.class, () -> service.overview(query)).getCode());
        verifyNoInteractions(mapper);
    }

    @Test
    void reversedDateAndInvalidEquipmentAreRejected()
    {
        var query = new StatisticsQuery(); query.setBeginDate("2026-01-02"); query.setEndDate("2026-01-01");
        assertEquals(400, assertThrows(ServiceException.class, () -> service.overview(query)).getCode());
        var invalid = new StatisticsQuery(); invalid.setEquipmentId(0L);
        assertEquals(400, assertThrows(ServiceException.class, () -> service.overview(invalid)).getCode());
        verifyNoInteractions(mapper);
    }

    @Test
    void oneDayIsHalfOpenAndEquipmentCountsStayGlobalWithDictionaryFallback()
    {
        var query = new StatisticsQuery(); query.setBeginDate("2026-01-01"); query.setEndDate("2026-01-01"); query.setEquipmentId(7L);
        when(mapper.selectEquipmentStatuses()).thenReturn(List.of(new CountBucket("IN_SERVICE",null,3),new CountBucket("RETIRED",null,2)));
        when(mapper.selectFaultCategories(query)).thenReturn(List.of(new CountBucket("LEGACY",null,1)));
        var result = service.overview(query);
        assertEquals(86400000L, query.getEndTime().getTime() - query.getBeginTime().getTime());
        assertEquals(5, result.getEquipmentTotal());
        assertEquals("LEGACY", result.getFaultCategories().get(0).getLabel());
        assertEquals("IN_SERVICE", result.getEquipmentStatuses().get(0).getLabel());
        verify(mapper).selectSummary(query);
        verify(mapper).selectEquipmentStatuses();
    }
}
