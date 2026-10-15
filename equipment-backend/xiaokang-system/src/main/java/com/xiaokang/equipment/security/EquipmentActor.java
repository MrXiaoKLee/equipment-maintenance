package com.xiaokang.equipment.security;

import java.util.Objects;
import com.xiaokang.equipment.domain.WorkOrder;

/** 服务端从当前账号和角色构造，不从请求绑定。 */
public class EquipmentActor
{
    private final Long userId;
    private final String userName;
    private final String displayName;
    private final boolean manager;
    private final boolean reporter;
    private final boolean maintainer;

    public EquipmentActor(Long userId, String userName, String displayName,
            boolean manager, boolean reporter, boolean maintainer)
    {
        this.userId = userId;
        this.userName = userName;
        this.displayName = displayName;
        this.manager = manager;
        this.reporter = reporter;
        this.maintainer = maintainer;
    }

    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getDisplayName() { return displayName; }
    public boolean isManager() { return manager; }
    public boolean isReporter() { return reporter; }
    public boolean isMaintainer() { return maintainer; }

    public boolean canView(WorkOrder order)
    {
        return manager || reporter && Objects.equals(userId, order.getReporterId())
                || maintainer && Objects.equals(userId, order.getHandlerId());
    }
}
