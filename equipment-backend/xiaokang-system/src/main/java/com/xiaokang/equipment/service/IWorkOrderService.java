package com.xiaokang.equipment.service;

import java.util.List;
import com.xiaokang.equipment.domain.WorkOrder;
import com.xiaokang.equipment.domain.WorkOrderRecord;
import com.xiaokang.equipment.dto.WorkOrderAssignRequest;
import com.xiaokang.equipment.dto.WorkOrderCancelRequest;
import com.xiaokang.equipment.dto.WorkOrderCreateRequest;
import com.xiaokang.equipment.dto.WorkOrderQuery;
import com.xiaokang.equipment.dto.WorkOrderProcessRequest;
import com.xiaokang.equipment.dto.WorkOrderSubmitReviewRequest;
import com.xiaokang.equipment.dto.WorkOrderReviewRequest;
import com.xiaokang.equipment.vo.MaintainerOption;
import com.xiaokang.equipment.vo.WorkOrderExportRow;

public interface IWorkOrderService
{
    List<WorkOrder> list(WorkOrderQuery query, int pageNum, int pageSize);
    List<WorkOrderExportRow> export(WorkOrderQuery query);
    WorkOrder get(Long id);
    List<WorkOrderRecord> records(Long id);
    List<MaintainerOption> maintainers(String keyword, int pageNum, int pageSize);
    WorkOrder create(WorkOrderCreateRequest request);
    WorkOrder assign(Long id, WorkOrderAssignRequest request);
    WorkOrder cancel(Long id, WorkOrderCancelRequest request);
    WorkOrder process(Long id, WorkOrderProcessRequest request);
    WorkOrder submitReview(Long id, WorkOrderSubmitReviewRequest request);
    WorkOrder review(Long id, WorkOrderReviewRequest request);
}
