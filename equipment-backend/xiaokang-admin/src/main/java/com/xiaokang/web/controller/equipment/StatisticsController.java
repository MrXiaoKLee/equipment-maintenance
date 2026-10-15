package com.xiaokang.web.controller.equipment;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xiaokang.common.core.controller.BaseController;
import com.xiaokang.common.core.domain.AjaxResult;
import com.xiaokang.equipment.dto.StatisticsQuery;
import com.xiaokang.equipment.service.StatisticsService;

@RestController
@RequestMapping("/equipment/statistics")
public class StatisticsController extends BaseController
{
    private final StatisticsService service;

    public StatisticsController(StatisticsService service) { this.service = service; }

    @PreAuthorize("@ss.hasPermi('equipment:statistics:query')")
    @GetMapping("/overview")
    public AjaxResult overview(StatisticsQuery query) { return success(service.overview(query)); }
}
