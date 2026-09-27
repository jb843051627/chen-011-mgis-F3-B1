-- ----------------------------------------------------------------------------
-- 整改回执单（质量监督处规矩落库）
--
-- 口径一句话：一批成果从挂出问题到销号，全程只凭系统里这一张整改回执单，
-- 一单对一批，单上分六站：挂号、告知、整改、复测、销号、封存。
-- 落笔链只增不改：经办、时辰在落笔那一下由系统写死，事后由不得人回头补录，
-- 也不许挪时辰；封存后顺拉倒拉各验一遍，少一笔、次序错一处即不合格。
-- ----------------------------------------------------------------------------

-- 回执单：一单对一批；批号 + 在途标记联合唯一——在途第二张立不出来
CREATE TABLE IF NOT EXISTS `t_mgis_rectify_order` (
  `id`              BIGINT       NOT NULL COMMENT '主键',
  `order_no`        VARCHAR(64)  NOT NULL COMMENT '单号',
  `batch_no`        VARCHAR(64)  NOT NULL COMMENT '成果批号(一单对一批)',
  `station`         VARCHAR(16)  NOT NULL COMMENT '眼下停在哪一站 NOTIFY告知 RECTIFY整改 RETEST复测 CLOSE销号 ARCHIVE封存',
  `round_no`        INT          NOT NULL DEFAULT 1 COMMENT '第几轮(退回重办一轮加一)',
  `status`          INT          NOT NULL DEFAULT 0 COMMENT '状态 0在途 1封存',
  `in_flight`       INT          NULL COMMENT '在途标记 1在途 NULL封存；与批号联合唯一，在途第二张立不出来',
  `detect_date`     DATE         NOT NULL COMMENT '归集的检出日(前一日)',
  `item_count`      INT          NOT NULL DEFAULT 0 COMMENT '归集件数',
  `notify_maps`     TEXT         NULL COMMENT '告知快照:问题落在哪几幅图(落笔即定,往后只读)',
  `notify_sampling` TEXT         NULL COMMENT '告知快照:复测按什么口径抽样(落笔即定,往后只读)',
  `notify_evidence` TEXT         NULL COMMENT '告知快照:复测须见着的凭证(落笔即定,往后只读)',
  `notify_by`       VARCHAR(64)  NULL COMMENT '告知经办(快照)',
  `notify_at`       DATETIME     NULL COMMENT '告知落笔时辰(快照)',
  `create_by`       VARCHAR(64)  NULL,
  `create_time`     DATETIME     NULL,
  `update_by`       VARCHAR(64)  NULL,
  `update_time`     DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rectify_order_no` (`order_no`),
  UNIQUE KEY `uk_rectify_batch_inflight` (`batch_no`, `in_flight`),
  KEY `idx_rectify_order_station` (`station`),
  KEY `idx_rectify_order_detect` (`detect_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整改回执单';

-- 落笔链：只增不改——不设 update_by/update_time/del_flag，服务层无改删入口
CREATE TABLE IF NOT EXISTS `t_mgis_rectify_entry` (
  `id`                  BIGINT      NOT NULL COMMENT '主键',
  `order_id`            BIGINT      NOT NULL COMMENT '回执单主键',
  `seq`                 INT         NOT NULL COMMENT '链上序号(自1连排)',
  `round_no`            INT         NOT NULL DEFAULT 1 COMMENT '第几轮(退回重办一轮加一)',
  `station`             VARCHAR(16) NOT NULL COMMENT '落笔站 REGISTER挂号 NOTIFY告知 RECTIFY整改 RETEST复测 CLOSE销号 ARCHIVE封存',
  `action`              VARCHAR(20) NOT NULL COMMENT '落笔动作 REGISTER/NOTIFY/RECTIFY/RETEST_AGENCY/RETEST_CITY/SEND_BACK/CLOSE/ARCHIVE',
  `payload`             TEXT        NULL COMMENT '这一笔写的话(JSON,落笔即定)',
  `penned_by`           VARCHAR(64) NOT NULL COMMENT '经办(落笔那一下系统写死)',
  `penned_at`           DATETIME    NOT NULL COMMENT '落笔时辰(系统写死,以秒计)',
  `prev_hash`           CHAR(64)    NOT NULL COMMENT '前一笔哈希(头一笔为64个0)',
  `hash`                CHAR(64)    NOT NULL COMMENT '本笔哈希',
  `supersedes_entry_id` BIGINT      NULL COMMENT '重办顶替的那一笔(被顶替的退出页面,链上仍在)',
  `create_by`           VARCHAR(64) NULL,
  `create_time`         DATETIME    NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rectify_entry_seq` (`order_id`, `seq`),
  UNIQUE KEY `uk_rectify_entry_point` (`order_id`, `station`, `action`, `round_no`),
  KEY `idx_rectify_entry_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整改回执单落笔链(只增不改)';

-- 归集件：立单那一刻挂进单子的检出件，只增不改
CREATE TABLE IF NOT EXISTS `t_mgis_rectify_item` (
  `id`            BIGINT      NOT NULL COMMENT '主键',
  `order_id`      BIGINT      NOT NULL COMMENT '回执单主键',
  `conclusion_id` BIGINT      NOT NULL COMMENT '判定结论主键',
  `item_code`     VARCHAR(64) NOT NULL COMMENT '图幅成果代号',
  `create_by`     VARCHAR(64) NULL,
  `create_time`   DATETIME    NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rectify_item` (`order_id`, `conclusion_id`),
  KEY `idx_rectify_item_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整改回执单归集件';

-- ----------------------------------------------------------------------------
-- 判定结论表补批号：不合格件夜里归集立整改单时按批归堆（空着的不归集）。
-- 老库执行下面两句；新库若已按 sql/mgis_qual.sql 最新建表脚本带出 batch_no 则跳过。
-- ----------------------------------------------------------------------------
-- ALTER TABLE `t_mgis_qual_conclusion` ADD COLUMN `batch_no` VARCHAR(64) NULL COMMENT '成果批号(整改零点归集按批立单)' AFTER `item_code`;
-- ALTER TABLE `t_mgis_qual_conclusion` ADD KEY `idx_qual_conclusion_detect` (`tier`, `issued_at`);
