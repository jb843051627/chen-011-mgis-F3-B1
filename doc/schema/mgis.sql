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
  due_at datetime DEFAULT NULL COMMENT '本项应当到期的日终截止时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '单条提前提醒的自然日数',
  status int DEFAULT NULL COMMENT '条目情形 0待提醒 1已提醒 2送不出去',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
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

