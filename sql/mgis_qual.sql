-- ----------------------------------------------------------------------------
-- 测绘成果质检判定线 / 判定结论（质量监督处规矩落库）
--
-- 口径一句话：出结论那一刻，由系统把所依据判定线的代号与启用之日写进结论，
-- 事后谁也不许手工补盖；同一幅图今天出与明天出，这一栏须一个字不差。
-- ----------------------------------------------------------------------------

-- 判定线表（新库直接建）
CREATE TABLE IF NOT EXISTS `t_mgis_qual_line` (
  `id`          BIGINT       NOT NULL COMMENT '主键',
  `rule_code`   VARCHAR(64)  NOT NULL COMMENT '判定线代号',
  `rule_name`   VARCHAR(128) NOT NULL COMMENT '判定线名称',
  `prod_type`   VARCHAR(32)  NULL COMMENT '成果类别 TOPO_MAP地形图 IMAGE_MAP影像图 VECTOR_FEATURE矢量要素 PLACE_NAME地名注记',
  `scale_min`   INT          NULL COMMENT '适用比例尺档下界(分母,含)；与上界同空=未设适用档，不当通吃',
  `scale_max`   INT          NULL COMMENT '适用比例尺档上界(分母,含)',
  `th1_max`     DECIMAL(18,6) NOT NULL COMMENT '一档中误差天花板(米)',
  `th2_max`     DECIMAL(18,6) NOT NULL COMMENT '二档中误差天花板(米)',
  `th3_max`     DECIMAL(18,6) NULL COMMENT '三档中误差天花板(米)；留白认作无顶，不许解成断了档',
  `eff_start`   DATETIME     NOT NULL COMMENT '启用之日(含)',
  `eff_end`     DATETIME     NULL COMMENT '交棒之日(不含)：当日起归新线；空=现行',
  `priority`    INT          NOT NULL DEFAULT 0 COMMENT '争档顺位值(大者优先；并列认代号靠后)',
  `status`      INT          NULL DEFAULT 0 COMMENT '0当家 1已让位（仅台账展示，判定一律以启用/交棒之日为准）',
  `del_flag`    INT          NULL DEFAULT 0 COMMENT '删除标记 0正常 1删除',
  `create_by`   VARCHAR(64)  NULL,
  `create_time` DATETIME     NULL,
  `update_by`   VARCHAR(64)  NULL,
  `update_time` DATETIME     NULL,
  `remark`      VARCHAR(500) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_qual_line_type_scale` (`prod_type`, `scale_min`, `scale_max`),
  KEY `idx_qual_line_eff` (`eff_start`, `eff_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测绘成果质检判定线';

-- 老库补列（t_mgis_qual_line 已存在时执行这三句即可）
-- ALTER TABLE `t_mgis_qual_line` ADD COLUMN `prod_type` VARCHAR(32) NULL COMMENT '成果类别' AFTER `rule_name`;
-- ALTER TABLE `t_mgis_qual_line` ADD COLUMN `scale_min` INT NULL COMMENT '适用比例尺档下界(分母,含)' AFTER `prod_type`;
-- ALTER TABLE `t_mgis_qual_line` ADD COLUMN `scale_max` INT NULL COMMENT '适用比例尺档上界(分母,含)' AFTER `scale_min`;

-- 判定结论表：只立不改——不设 update_by/update_time/del_flag，服务层无改删入口
CREATE TABLE IF NOT EXISTS `t_mgis_qual_conclusion` (
  `id`                BIGINT       NOT NULL COMMENT '主键',
  `item_code`         VARCHAR(64)  NOT NULL COMMENT '图幅成果代号',
  `batch_no`          VARCHAR(64)  NULL COMMENT '成果批号(整改零点归集按批立单；空着的不归集)',
  `prod_type`         VARCHAR(32)  NOT NULL COMMENT '成果类别',
  `scale_denominator` INT          NOT NULL COMMENT '比例尺分母',
  `submit_date`       DATE         NOT NULL COMMENT '交件日期(判定以交件日为准)',
  `mean_error`        DECIMAL(18,6) NOT NULL COMMENT '中误差(米)',
  `rule_id`           BIGINT       NULL COMMENT '判定线主键(备查；判定依据以快照栏为准)',
  `rule_code`         VARCHAR(64)  NOT NULL COMMENT '判定线代号(快照，落账即定)',
  `rule_name`         VARCHAR(128) NULL COMMENT '判定线名称(快照)',
  `rule_eff_start`    DATETIME     NULL COMMENT '判定线启用之日(快照，落账即定)',
  `tier`              INT          NOT NULL COMMENT '档位 1一档 2二档 3三档',
  `on_ceiling`        INT          NOT NULL DEFAULT 0 COMMENT '是否压线 0否 1是',
  `pick_reason`       VARCHAR(500) NULL COMMENT '选定缘由：凭什么是这条线',
  `issued_at`         DATETIME     NOT NULL COMMENT '出结论时刻',
  `create_by`         VARCHAR(64)  NULL,
  `create_time`       DATETIME     NULL,
  PRIMARY KEY (`id`),
  KEY `idx_qual_conclusion_item` (`item_code`),
  KEY `idx_qual_conclusion_issued` (`issued_at`),
  KEY `idx_qual_conclusion_detect` (`tier`, `issued_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测绘成果质检判定结论';
