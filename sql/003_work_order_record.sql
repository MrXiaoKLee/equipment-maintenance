-- P3追加迁移。先执行001、002，再在equipment_maintenance项目库执行本脚本。
CREATE TABLE IF NOT EXISTS eq_work_order_record (
    record_id BIGINT NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    from_status VARCHAR(32) DEFAULT NULL,
    to_status VARCHAR(32) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    cause_analysis VARCHAR(2000) DEFAULT NULL,
    measures VARCHAR(2000) DEFAULT NULL,
    result_description VARCHAR(2000) DEFAULT NULL,
    actor_id BIGINT NOT NULL,
    actor_name VARCHAR(100) NOT NULL,
    create_time DATETIME NOT NULL,
    PRIMARY KEY (record_id),
    KEY idx_eq_record_order_time (work_order_id,create_time,record_id),
    CONSTRAINT fk_eq_record_order FOREIGN KEY (work_order_id) REFERENCES eq_work_order(work_order_id),
    CONSTRAINT fk_eq_record_actor FOREIGN KEY (actor_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单追加式动作记录';
