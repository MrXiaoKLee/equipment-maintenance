package com.xiaokang.web.controller.equipment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.xiaokang.common.core.domain.AjaxResult;

/** 业务接口的解析错误与持久化错误不向页面暴露内部细节。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = WorkOrderController.class)
public class EquipmentRequestAdvice
{
    private static final Logger log = LoggerFactory.getLogger(EquipmentRequestAdvice.class);

    @ExceptionHandler({HttpMessageNotReadableException.class, BindException.class,
            MethodArgumentTypeMismatchException.class})
    public AjaxResult invalidRequest(Exception exception)
    {
        return AjaxResult.error(400, "请求参数格式不正确，请检查编号、日期或必填字段");
    }

    @ExceptionHandler(DataAccessException.class)
    public AjaxResult persistenceFailure(DataAccessException exception)
    {
        log.error("设备维护数据操作失败", exception);
        return AjaxResult.error(500, "数据操作失败，请稍后重试或联系管理员");
    }
}
