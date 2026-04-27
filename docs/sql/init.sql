-- =====================================================
-- MARL-TSC 数据库初始化脚本
-- MySQL 8.0+  required
-- 数据库: marl_tsc
-- =====================================================

CREATE DATABASE IF NOT EXISTS marl_tsc
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE marl_tsc;

-- ---------------------------------------------------
-- 1. 信号方案表
-- ---------------------------------------------------
DROP TABLE IF EXISTS signal_plan;
CREATE TABLE signal_plan (
    plan_id         VARCHAR(50)  PRIMARY KEY COMMENT '方案ID',
    intersection_id VARCHAR(50)  NOT NULL        COMMENT '路口ID',
    plan_name       VARCHAR(100)                 COMMENT '方案名称',
    cycle_time      INT          NOT NULL DEFAULT 60 COMMENT '周期(秒)',
    status          TINYINT      DEFAULT 0        COMMENT '0=停用 1=启用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_intersection (intersection_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信号方案';

-- ---------------------------------------------------
-- 2. 信号相位表
-- ---------------------------------------------------
DROP TABLE IF EXISTS signal_phase;
CREATE TABLE signal_phase (
    phase_id    INT AUTO_INCREMENT PRIMARY KEY COMMENT '相位ID',
    plan_id     VARCHAR(50)  NOT NULL        COMMENT '所属方案ID',
    direction   VARCHAR(20)  NOT NULL        COMMENT '方向(东西/南北)',
    green_time  INT          NOT NULL DEFAULT 30 COMMENT '绿灯时间(秒)',
    yellow_time INT          NOT NULL DEFAULT 3  COMMENT '黄灯时间(秒)',
    red_time    INT          NOT NULL DEFAULT 30 COMMENT '红灯时间(秒)',
    sequence    INT          NOT NULL DEFAULT 0  COMMENT '相位顺序',
    FOREIGN KEY (plan_id) REFERENCES signal_plan(plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信号相位';

-- ---------------------------------------------------
-- 3. 协同优化日志表
-- ---------------------------------------------------
DROP TABLE IF EXISTS optimization_log;
CREATE TABLE optimization_log (
    log_id             VARCHAR(50)  PRIMARY KEY COMMENT '日志ID',
    trigger_time       DATETIME     NOT NULL    COMMENT '触发时间',
    intersection_id    VARCHAR(50)  NOT NULL    COMMENT '路口ID',
    joint_reward       DOUBLE                   COMMENT '联合奖励值',
    signal_action_json TEXT                     COMMENT '信号动作JSON',
    route_action_json  TEXT                     COMMENT '路径动作JSON',
    status             TINYINT      DEFAULT 0   COMMENT '状态',
    INDEX idx_intersection (intersection_id),
    INDEX idx_trigger_time (trigger_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协同优化日志';

-- ---------------------------------------------------
-- 4. 交通流特征表
-- ---------------------------------------------------
DROP TABLE IF EXISTS traffic_feature;
CREATE TABLE traffic_feature (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    intersection_id  VARCHAR(50) NOT NULL              COMMENT '路口ID',
    flow             DOUBLE                            COMMENT '车流量(辆/时)',
    speed            DOUBLE                            COMMENT '平均速度(km/h)',
    occupancy        DOUBLE                            COMMENT '占用率(0~1)',
    queue_length     DOUBLE                            COMMENT '排队长度(m)',
    delay            DOUBLE                            COMMENT '延误(秒)',
    record_time      DATETIME   NOT NULL               COMMENT '记录时间',
    create_time      DATETIME   DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_intersection_record (intersection_id, record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交通流特征';

-- ---------------------------------------------------
-- 初始演示数据
-- ---------------------------------------------------

-- 信号方案
INSERT INTO signal_plan (plan_id, intersection_id, plan_name, cycle_time, status) VALUES
    ('PLAN-INT-001', 'INT-001', '路口A默认方案', 90, 1),
    ('PLAN-INT-002', 'INT-002', '路口B默认方案', 90, 1),
    ('PLAN-INT-003', 'INT-003', '路口C默认方案', 90, 1);

-- 信号相位
INSERT INTO signal_phase (plan_id, direction, green_time, yellow_time, red_time, sequence) VALUES
    ('PLAN-INT-001', '东西', 35, 3, 52, 1),
    ('PLAN-INT-001', '南北', 40, 3, 47, 2),
    ('PLAN-INT-002', '东西', 30, 3, 57, 1),
    ('PLAN-INT-002', '南北', 45, 3, 42, 2),
    ('PLAN-INT-003', '东西', 35, 3, 52, 1),
    ('PLAN-INT-003', '南北', 35, 3, 52, 2);
