package com.xiaokang.equipment.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import com.xiaokang.common.exception.ServiceException;

/** 列表、导出、统计共同使用北京时间左闭右开日期边界。 */
public class ReportingPeriod
{
    private final Date beginTime;
    private final Date endTime;

    private ReportingPeriod(Date beginTime, Date endTime)
    {
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    public Date getBeginTime() { return beginTime; }
    public Date getEndTime() { return endTime; }

    public static ReportingPeriod parse(String beginDate, String endDate)
    {
        Date begin = boundary(beginDate, false);
        Date end = boundary(endDate, true);
        if (begin != null && end != null && !begin.before(end))
        {
            throw new ServiceException("开始日期不能晚于结束日期", 400);
        }
        return new ReportingPeriod(begin, end);
    }

    private static Date boundary(String value, boolean end)
    {
        if (value == null || value.isBlank()) return null;
        try
        {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            if (date.getYear() < 1000 || date.getYear() > 9998) throw new DateTimeParseException("range", value, 0);
            return Date.from((end ? date.plusDays(1) : date).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
        }
        catch (DateTimeParseException ex)
        {
            throw new ServiceException("日期应为yyyy-MM-dd格式的有效日期", 400);
        }
    }
}
