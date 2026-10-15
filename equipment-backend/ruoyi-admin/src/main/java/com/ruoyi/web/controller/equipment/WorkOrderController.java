package com.ruoyi.web.controller.equipment;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.equipment.dto.WorkOrderAssignRequest;
import com.ruoyi.equipment.dto.WorkOrderCancelRequest;
import com.ruoyi.equipment.dto.WorkOrderCreateRequest;
import com.ruoyi.equipment.dto.WorkOrderQuery;
import com.ruoyi.equipment.service.IWorkOrderService;

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

    @PreAuthorize("@ss.hasPermi('equipment:workorder:cancel')")
    @Log(title = "取消报修", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id, @RequestBody WorkOrderCancelRequest request)
    {
        return success(service.cancel(id, request));
    }
}
