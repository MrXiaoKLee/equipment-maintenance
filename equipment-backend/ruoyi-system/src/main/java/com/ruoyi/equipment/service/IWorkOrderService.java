package com.ruoyi.equipment.service;

import java.util.List;
import com.ruoyi.equipment.domain.WorkOrder;
import com.ruoyi.equipment.domain.WorkOrderRecord;
import com.ruoyi.equipment.dto.WorkOrderAssignRequest;
import com.ruoyi.equipment.dto.WorkOrderCancelRequest;
import com.ruoyi.equipment.dto.WorkOrderCreateRequest;
import com.ruoyi.equipment.dto.WorkOrderQuery;
import com.ruoyi.equipment.dto.WorkOrderProcessRequest;
import com.ruoyi.equipment.dto.WorkOrderSubmitReviewRequest;
import com.ruoyi.equipment.dto.WorkOrderReviewRequest;
import com.ruoyi.equipment.vo.MaintainerOption;
import com.ruoyi.equipment.vo.WorkOrderExportRow;

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
