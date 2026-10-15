package com.xiaokang.web.controller.equipment;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xiaokang.common.annotation.Log;
import com.xiaokang.common.core.controller.BaseController;
import com.xiaokang.common.core.domain.AjaxResult;
import com.xiaokang.common.core.page.PageDomain;
import com.xiaokang.common.core.page.TableDataInfo;
import com.xiaokang.common.core.page.TableSupport;
import com.xiaokang.common.enums.BusinessType;
import com.xiaokang.common.utils.poi.ExcelUtil;
import com.xiaokang.equipment.domain.Equipment;
import com.xiaokang.equipment.dto.EquipmentCreateRequest;
import com.xiaokang.equipment.dto.EquipmentEditRequest;
import com.xiaokang.equipment.dto.EquipmentQuery;
import com.xiaokang.equipment.dto.EquipmentStatusRequest;
import com.xiaokang.equipment.service.IEquipmentService;

/** 设备台账接口，不提供删除或任意修改状态的入口。 */
@RestController
@RequestMapping("/equipment/asset")
public class EquipmentController extends BaseController
{
    private final IEquipmentService service;

    public EquipmentController(IEquipmentService service)
    {
        this.service = service;
    }

    @PreAuthorize("@ss.hasPermi('equipment:asset:list')")
    @GetMapping("/list")
    public TableDataInfo list(EquipmentQuery query)
    {
        PageDomain page = TableSupport.buildPageRequest();
        return getDataTable(service.list(query, page.getPageNum(), page.getPageSize()));
    }

    @PreAuthorize("@ss.hasPermi('equipment:asset:query')")
    @GetMapping("/options")
    public TableDataInfo options(EquipmentQuery query)
    {
        query.setEquipmentStatus("IN_SERVICE");
        PageDomain page = TableSupport.buildPageRequest();
        return getDataTable(service.list(query, page.getPageNum(), page.getPageSize()));
    }

    @PreAuthorize("@ss.hasPermi('equipment:asset:query')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id)
    {
        return success(service.get(id));
    }

    @PreAuthorize("@ss.hasPermi('equipment:asset:add')")
    @Log(title = "设备台账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@RequestBody EquipmentCreateRequest request)
    {
        return success(service.create(request));
    }

    @PreAuthorize("@ss.hasPermi('equipment:asset:edit')")
    @Log(title = "设备台账", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody EquipmentEditRequest request)
    {
        return success(service.edit(id, request));
    }

    @PreAuthorize("@ss.hasPermi('equipment:asset:status')")
    @Log(title = "设备使用状态", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult status(@PathVariable Long id, @RequestBody EquipmentStatusRequest request)
    {
        return success(service.changeStatus(id, request));
    }

    @PreAuthorize("@ss.hasPermi('equipment:workorder:list')")
    @GetMapping("/{id}/work-orders")
    public TableDataInfo history(@PathVariable Long id)
    {
        PageDomain page = TableSupport.buildPageRequest();
        return getDataTable(service.history(id, page.getPageNum(), page.getPageSize()));
    }

    @PreAuthorize("@ss.hasPermi('equipment:asset:export')")
    @Log(title = "设备台账", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EquipmentQuery query)
    {
        List<Equipment> rows = service.export(query);
        new ExcelUtil<>(Equipment.class).exportExcel(response, rows, "设备台账");
    }

}
