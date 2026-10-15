package com.xiaokang.web.controller.equipment;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xiaokang.common.annotation.Log;
import com.xiaokang.common.core.controller.BaseController;
import com.xiaokang.common.core.domain.AjaxResult;
import com.xiaokang.common.core.page.PageDomain;
import com.xiaokang.common.core.page.TableDataInfo;
import com.xiaokang.common.core.page.TableSupport;
import com.xiaokang.common.enums.BusinessType;
import com.xiaokang.equipment.dto.WorkOrderAssignRequest;
import com.xiaokang.equipment.dto.WorkOrderCancelRequest;
import com.xiaokang.equipment.dto.WorkOrderCreateRequest;
import com.xiaokang.equipment.dto.WorkOrderQuery;
import com.xiaokang.equipment.dto.WorkOrderProcessRequest;
import com.xiaokang.equipment.dto.WorkOrderSubmitReviewRequest;
import com.xiaokang.equipment.dto.WorkOrderReviewRequest;
import com.xiaokang.equipment.service.IWorkOrderService;
import com.xiaokang.equipment.vo.WorkOrderExportRow;
import com.xiaokang.common.utils.poi.ExcelUtil;

@RestController
@RequestMapping("/equipment/workorder")
public class WorkOrderController extends BaseController
{
    private final IWorkOrderService service;

    public WorkOrderController(IWorkOrderService service)
    {
        this.service = service;
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkOrderQuery query)
    {
        PageDomain page = TableSupport.buildPageRequest();
        return getDataTable(service.list(query, page.getPageNum(), page.getPageSize()));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:query')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id)
    {
        return success(service.get(id));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:export')")
    @Log(title = "故障工单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WorkOrderQuery query)
    {
        new ExcelUtil<>(WorkOrderExportRow.class).exportExcel(response, service.export(query), "故障工单");
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:query')")
    @GetMapping("/{id}/records")
    public AjaxResult records(@PathVariable Long id)
    {
        return success(service.records(id));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:assign')")
    @GetMapping("/maintainers")
    public TableDataInfo maintainers(@RequestParam(defaultValue = "") String keyword)
    {
        PageDomain page = TableSupport.buildPageRequest();
        return getDataTable(service.maintainers(keyword, page.getPageNum(), page.getPageSize()));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:add')")
    @Log(title = "故障报修", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@RequestBody WorkOrderCreateRequest request)
    {
        return success(service.create(request));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:assign')")
    @Log(title = "分配维护人员", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/assign")
    public AjaxResult assign(@PathVariable Long id, @RequestBody WorkOrderAssignRequest request)
    {
        return success(service.assign(id, request));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:process')")
    @Log(title = "追加处理记录", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/records")
    public AjaxResult process(@PathVariable Long id, @RequestBody WorkOrderProcessRequest request)
    {
        return success(service.process(id, request));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:submit')")
    @Log(title = "提交处理结果", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/submit-review")
    public AjaxResult submitReview(@PathVariable Long id, @RequestBody WorkOrderSubmitReviewRequest request)
    {
        return success(service.submitReview(id, request));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:review')")
    @Log(title = "复核处理结果", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/review")
    public AjaxResult review(@PathVariable Long id, @RequestBody WorkOrderReviewRequest request)
    {
        return success(service.review(id, request));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:cancel')")
    @Log(title = "取消报修", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id, @RequestBody WorkOrderCancelRequest request)
    {
        return success(service.cancel(id, request));
    }
}
