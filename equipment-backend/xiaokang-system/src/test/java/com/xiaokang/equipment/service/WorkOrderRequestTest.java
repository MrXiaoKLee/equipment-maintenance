package com.xiaokang.equipment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaokang.equipment.dto.WorkOrderCancelRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkOrderRequestTest
{
    @Test
    void cancellationAcceptsDocumentedReasonField() throws Exception
    {
        WorkOrderCancelRequest request = new ObjectMapper().readValue(
                "{\"reason\":\"模拟误报\",\"version\":0}", WorkOrderCancelRequest.class);
        assertEquals("模拟误报", request.getReason());
        assertEquals(0, request.getVersion());
    }

    @Test
    void cancellationAcceptsLegacyDescriptionAlias() throws Exception
    {
        WorkOrderCancelRequest request = new ObjectMapper().readValue(
                "{\"description\":\"模拟误报\",\"version\":0}", WorkOrderCancelRequest.class);
        assertEquals("模拟误报", request.getReason());
    }
}
