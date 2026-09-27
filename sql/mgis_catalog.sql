-- ----------------------------------------------------------------------------
-- 年度图幅目录册核收（成果管理处规矩落库）
--
-- 口径一句话：一册整批递进来，系统逐行解析一遍——行次、图幅代号、成果类别、
-- 属地名址四样跟着每一行走；收下的进账，没收下的挂账，挂账行次、代号、缘故
-- 三样齐着写。一处填错不连坐整册；同册号重递只认最早那一遍；同图幅代号占两行
-- 并成一条进账、幅数只加一次；越线整册不收、白表不许回「已核讫」。
-- 只立不改：不设 update_by/update_time/del_flag，服务层无单条添改、删除入口。
-- ----------------------------------------------------------------------------

-- 册子：一册递进来立一条；册号唯一——重递只认最早那一遍，后到的顶不掉先收的
CREATE TABLE IF NOT EXISTS `t_mgis_catalog_book` (
  `id`            BIGINT       NOT NULL COMMENT '主键',
  `book_no`       VARCHAR(64)  NOT NULL COMMENT '册号(一册一号,唯一;重递只认头一遍)',
  `submit_region` VARCHAR(128) NOT NULL COMMENT '递交属地(十一市册子抬头)',
  `total_rows`    INT          NOT NULL COMMENT '表上总行数(系统按原表逐行数得)',
  `accepted_rows` INT          NOT NULL DEFAULT 0 COMMENT '进账行数(挂账+进账=总行数;被并掉的重复行仍算进账行)',
  `pending_rows`  INT          NOT NULL DEFAULT 0 COMMENT '挂账行数(一处填错只挂这一行,不连坐)',
  `entry_count`   INT          NOT NULL DEFAULT 0 COMMENT '进账条目数(同图幅代号并成一条后的条数)',
  `sheet_total`   INT          NOT NULL DEFAULT 0 COMMENT '进账幅数合计(同代号只按头一行加一次)',
  `create_by`     VARCHAR(64)  NULL,
  `create_time`   DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_book_no` (`book_no`),
  KEY `idx_catalog_book_region` (`submit_region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度图幅目录册(只立不改)';

-- 行目：原表每一行落一条，进账、挂账都在这张表上——挂账加入账当场对得上总行数
CREATE TABLE IF NOT EXISTS `t_mgis_catalog_line` (
  `id`               BIGINT       NOT NULL COMMENT '主键',
  `book_id`          BIGINT       NOT NULL COMMENT '册子主键',
  `book_no`          VARCHAR(64)  NOT NULL COMMENT '册号(冗余,凭册号直接翻行目)',
  `row_no`           INT          NOT NULL COMMENT '原表行次(填报所指;未填由系统按序补)',
  `sheet_code`       VARCHAR(64)  NULL COMMENT '图幅代号(挂账行照留填报原值,空缺即为空)',
  `prod_type`        VARCHAR(32)  NULL COMMENT '成果类别 TOPO_MAP/IMAGE_MAP/VECTOR_FEATURE/PLACE_NAME;不识照留原值并挂账',
  `region`           VARCHAR(128) NULL COMMENT '属地名址',
  `sheet_count_raw`  VARCHAR(32)  NULL COMMENT '幅数填报原值(照原样留,负数/非数也追得到原笔)',
  `sheet_count`      INT          NULL COMMENT '解析后的幅数(进账行有值;挂账行为空)',
  `disposition`      VARCHAR(16)  NOT NULL COMMENT '归宿 ACCEPTED进账 PENDING挂账',
  `reason_code`      VARCHAR(32)  NULL COMMENT '挂账缘故代号 NEGATIVE_SHEET_COUNT/SHEET_COUNT_NOT_A_NUMBER/MISSING_SHEET_CODE/UNSUPPORTED_PROD_TYPE/MISSING_REGION',
  `reason_text`      VARCHAR(255) NULL COMMENT '挂账缘故(成文那一句,重递不再补第二句)',
  `primary_row_no`   INT          NULL COMMENT '进账组头行行次:同代号并一条,幅数只按头一行加;头行指自己,被并行指头行;挂账行为空',
  `create_by`        VARCHAR(64)  NULL,
  `create_time`      DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_line_row` (`book_id`, `row_no`),
  KEY `idx_catalog_line_book` (`book_id`),
  KEY `idx_catalog_line_region` (`region`),
  KEY `idx_catalog_line_code` (`book_no`, `sheet_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度图幅目录册行目(进账挂账同表,只立不改)';
