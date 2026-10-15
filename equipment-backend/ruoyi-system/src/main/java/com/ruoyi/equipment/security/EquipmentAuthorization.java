package com.ruoyi.equipment.security;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysUserService;

/** 设备与工单共用的即时账号、角色校验。 */
@Component
public class EquipmentAuthorization
{
    private final ISysUserService users;

    public EquipmentAuthorization(ISysUserService users)
    {
        this.users = users;
    }

    public EquipmentActor requireReader()
    {
        SysUser user = users.selectUserById(SecurityUtils.getUserId());
        if (user == null || !"0".equals(user.getStatus()) || !"0".equals(user.getDelFlag()))
        {
            throw new ServiceException("账号已停用或不存在", 403);
        }
        List<SysRole> roles = user.getRoles() == null ? Collections.emptyList() : user.getRoles();
        boolean admin = SecurityUtils.isAdmin();
        EquipmentActor actor = new EquipmentActor(user.getUserId(), user.getUserName(),
                user.getNickName() == null ? user.getUserName() : user.getNickName(),
                admin || hasRole(roles, "eq_manager"), admin || hasRole(roles, "eq_reporter"),
                admin || hasRole(roles, "eq_maintainer"));
        if (!actor.isManager() && !actor.isReporter() && !actor.isMaintainer())
        {
            throw new ServiceException("无设备维护业务访问权限", 403);
        }
        return actor;
    }

    public EquipmentActor requireManager()
    {
        EquipmentActor actor = requireReader();
        if (!actor.isManager())
        {
            throw new ServiceException("只有设备管理人员可以执行该操作", 403);
        }
        return actor;
    }

    public EquipmentActor requireReporter()
    {
        EquipmentActor actor = requireReader();
        if (!actor.isReporter())
        {
            throw new ServiceException("只有报修人员可以提交报修", 403);
        }
        return actor;
    }

    private boolean hasRole(List<SysRole> roles, String key)
    {
        return roles.stream().anyMatch(role -> key.equals(role.getRoleKey())
                && "0".equals(role.getStatus()) && "0".equals(role.getDelFlag()));
    }
}
