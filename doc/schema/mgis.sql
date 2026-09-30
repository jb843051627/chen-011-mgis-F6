-- mgis 测绘地理信息成果提供使用与测绘资质监督管理 -- schema (chen-011)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_011

CREATE TABLE IF NOT EXISTS t_mgis_check_card (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '不合格成果整改回执号',
  stage int DEFAULT NULL COMMENT '当前关口 0..5',
  status int DEFAULT NULL COMMENT '回执落定 0在办 1已销号 2已封存',
  content varchar(255) DEFAULT NULL COMMENT '整改记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不合格成果整改回执单';

CREATE TABLE IF NOT EXISTS t_mgis_duty_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '到期提醒单号',
  due_at datetime DEFAULT NULL COMMENT '本项应当到期的日终截止时刻(语义恒为到期当日23:59:59，只填了日子的历史条目按此刻归一)',
  amount decimal(12,2) DEFAULT NULL COMMENT '处里定的提前开口自然日数(口径要挪只改此数，不动日终时刻)',
  city_code varchar(32) DEFAULT NULL COMMENT '归口地市代码',
  city_name varchar(64) DEFAULT NULL COMMENT '归口地市名称',
  liaison varchar(64) DEFAULT NULL COMMENT '归口联络员(空着=人调走/岗位并了，本条转挂起，不碍其余各条)',
  status int DEFAULT '0' COMMENT '条目情形 0待提醒 1已提醒 2已挂起',
  suspend_reason varchar(255) DEFAULT NULL COMMENT '挂起缘故(归口为空等)',
  sent_at datetime DEFAULT NULL COMMENT '实际发出时刻(回写成功才落，回写不成仍按未发)',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  KEY idx_duty_status_due (status, due_at),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='到期提醒派发条目';

CREATE TABLE IF NOT EXISTS t_mgis_qual_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '成果质检判定线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '判定线名称',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '一档中误差限差上限(米)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档中误差限差上限(米)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档中误差限差上限(米)',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '抢档顺位(数值越大越优先)',
  status int DEFAULT NULL COMMENT '线的情形 0当家 1已让位',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='测绘成果质检判定线';

CREATE TABLE IF NOT EXISTS t_mgis_scope_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '作业范围标签卡号',
  site_id int DEFAULT NULL COMMENT '所属资质单位基础档案',
  site_no varchar(64) DEFAULT NULL COMMENT '资质单位编号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡挂用专业类别总数(项)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本年度新增挂用类别数(项)',
  grade_level int DEFAULT NULL COMMENT '标签层级档',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已定档',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业范围标签建档卡';

CREATE TABLE IF NOT EXISTS t_mgis_submit_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '成果目录汇交册号',
  row_no int DEFAULT NULL COMMENT '原册内行次',
  item_code varchar(64) DEFAULT NULL COMMENT '图幅成果代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行填报图幅幅数(幅)',
  status int DEFAULT NULL COMMENT '行进展 0待销 1已入账 2挂驳回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成果目录汇交核收行';

CREATE TABLE IF NOT EXISTS t_mgis_unit_doc (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '资质单位编号',
  site_name varchar(128) DEFAULT NULL COMMENT '资质单位基础档案名称',
  site_type varchar(32) DEFAULT NULL COMMENT '单位性质类',
  road_name varchar(128) DEFAULT NULL COMMENT '注册地(省—市—区县)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '挂用专业类别数一档上限(项)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档上限(项)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档上限(项)',
  status int DEFAULT NULL COMMENT '档案情形 0在册 1已注销',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资质单位基础档案';

CREATE TABLE IF NOT EXISTS t_mgis_use_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '成果提供使用核准单号',
  node_no int DEFAULT NULL COMMENT '当前所在层 0..3',
  sign_mode int DEFAULT NULL COMMENT '同层核准方式 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本层应签人数',
  sign_count int DEFAULT NULL COMMENT '本层已签人数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成果提供使用逐级核准单';

-- 初始档案数据（在册 / 注销两种状态各一条）
-- t_mgis_unit_doc 两条种子档案：id=0 在册、id=1 已注销（在册与注销的联动样例）。
-- 挂用专业类别数上限取 30/120/360 项，取值达到上限时按高一档折算。
INSERT IGNORE INTO t_mgis_unit_doc (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'ZZ00', '江南省测绘院第一地理信息工程院基础档案', '事业单位', '江南省—临川市—城东区', 30.00, 120.00, 360.00, 0, 0, 'seed', NOW()),
       (1, 'ZZ01', '旧临川市城乡测绘服务中心基础档案（已注销）', '企业单位', '江南省—临川市—城西区', 30.00, 120.00, 360.00, 1, 0, 'seed', NOW());


-- ============================================================================
-- 五月处务会新规：到期提醒「系统自己找、自己发」
-- 旧版表结构升级（列已存在则跳过；可重复执行）
-- ============================================================================
DROP PROCEDURE IF EXISTS p_duty_task_upgrade;
DELIMITER $$
CREATE PROCEDURE p_duty_task_upgrade()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_mgis_duty_task'
                     AND COLUMN_NAME = 'city_code') THEN
        ALTER TABLE t_mgis_duty_task ADD COLUMN city_code varchar(32) DEFAULT NULL COMMENT '归口地市代码' AFTER amount;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_mgis_duty_task'
                     AND COLUMN_NAME = 'city_name') THEN
        ALTER TABLE t_mgis_duty_task ADD COLUMN city_name varchar(64) DEFAULT NULL COMMENT '归口地市名称' AFTER city_code;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_mgis_duty_task'
                     AND COLUMN_NAME = 'liaison') THEN
        ALTER TABLE t_mgis_duty_task ADD COLUMN liaison varchar(64) DEFAULT NULL COMMENT '归口联络员(空=转挂起)' AFTER city_name;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_mgis_duty_task'
                     AND COLUMN_NAME = 'suspend_reason') THEN
        ALTER TABLE t_mgis_duty_task ADD COLUMN suspend_reason varchar(255) DEFAULT NULL COMMENT '挂起缘故' AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_mgis_duty_task'
                     AND COLUMN_NAME = 'sent_at') THEN
        ALTER TABLE t_mgis_duty_task ADD COLUMN sent_at datetime DEFAULT NULL COMMENT '实际发出时刻' AFTER suspend_reason;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_mgis_duty_task'
                     AND INDEX_NAME = 'idx_duty_status_due') THEN
        ALTER TABLE t_mgis_duty_task ADD KEY idx_duty_status_due (status, due_at);
    END IF;
END$$
DELIMITER ;
CALL p_duty_task_upgrade();
DROP PROCEDURE IF EXISTS p_duty_task_upgrade;

-- 纸质清册搬进来的老条目只填了日子：DATE 入 datetime 落在 00:00:00（零点头一格），
-- 统一归一到当日 23:59:59。老条目与系统自己落的新条目此后同一把尺，不存在一处算压线一处算未到。
UPDATE t_mgis_duty_task
SET due_at = CONCAT(DATE(due_at), ' 23:59:59')
WHERE due_at IS NOT NULL AND TIME(due_at) = '00:00:00';

-- 各分支演示种子（日期随建库当日滚动，保证「压线当日」一条始终成立；INSERT IGNORE 可重复执行）
INSERT IGNORE INTO t_mgis_duty_task
(id, item_no, due_at, amount, city_code, city_name, liaison, status, suspend_reason, sent_at, del_flag, create_by, create_time, remark)
VALUES
(9101, 'TX-DEMO-0001', CONCAT(CURDATE(), ' 23:59:59'), 0.00,   'LC', '临川市', '王联络', 0, NULL, NULL, 0, 'seed', NOW(), '压线当日，提前0天，窗口内即发'),
(9102, 'TX-DEMO-0002', CONCAT(CURDATE(), ' 23:59:59'), 3.00,   'LC', '临川市', '王联络', 0, NULL, NULL, 0, 'seed', NOW(), '压线当日，提前3天，窗口内即发'),
(9103, 'TX-DEMO-0003', CONCAT(DATE_ADD(CURDATE(), INTERVAL 10 DAY), ' 23:59:59'), 3.00, 'LC', '临川市', '王联络', 0, NULL, NULL, 0, 'seed', NOW(), '未到开口日，本轮计入待发但不发'),
(9104, 'TX-DEMO-0004', CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 23:59:59'), 3.00, 'YC', '宜春市', '李联络', 0, NULL, NULL, 0, 'seed', NOW(), '已过期未发，开口中，窗口内即发'),
(9105, 'TX-DEMO-0005', CONCAT(CURDATE(), ' 23:59:59'), 3.00,   'SR', '上饶市', NULL,    0, NULL, NULL, 0, 'seed', NOW(), '归口空着（人调走/岗位并了），本轮转挂起'),
(9106, 'TX-DEMO-0006', CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 23:59:59'), 3.00, 'PY', '萍乡市', '赵联络', 1, NULL, CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 10:00:00'), 0, 'seed', NOW(), '已发条目，任何轮次不再挑出'),
(9107, 'TX-DEMO-0007', CONCAT(CURDATE(), ' 00:00:00'), 1.00,   'Jdz', '景德镇市', '孙联络', 0, NULL, NULL, 0, 'seed', NOW(), '纸质清册老条目，只填日子未填钟点');

-- 老条目（含上一行 9107 演示种子）统一归一日终，归一须在种子落库之后再走一遍
UPDATE t_mgis_duty_task
SET due_at = CONCAT(DATE(due_at), ' 23:59:59')
WHERE due_at IS NOT NULL AND TIME(due_at) = '00:00:00';
