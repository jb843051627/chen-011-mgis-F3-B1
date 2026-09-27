# 年度图幅目录册核收规矩 → 系统口径对照

成果管理处每年十月收全省十一市年度图幅目录册的规矩，在系统里只有一处出处：
`com.fc.v2.common.catalog.CatalogBookJudge`（核收推演）。整批提交门、翻账页面
全从这一处过——回执上的数字与库里的数字出自同一处计算，两处合不上便是事故，
所以别处不许另写一套。

一册整批递进来，系统逐行解析一遍：行次、图幅代号、成果类别、属地名址四样跟着
每一行走；收下的进账，没收下的挂账，挂账行次、代号、缘故三样齐着写。

| 处里的说法 | 系统口径 | 落点 |
| --- | --- | --- |
| 一册递进来逐行给结果 | 每一行由系统解析一遍，带着原表行次、图幅代号、成果类别、属地名址；进账行进账，挂账行单列，行次/代号/缘故三样齐着写，市里不必拿计算器逐行数 | `CatalogBookJudge.judge`、`t_mgis_catalog_line` |
| 一行负数，整册照走，那行自己挂着 | 一处填错不连坐：负数（以及非数、代号空、类别不识、属地空）只挂这一行，缘故齐着写、浑数带填报原值；其余行照常进账 | `CatalogBookJudge.judgeLine`、`parseSheetCount` |
| 同一册号送来第二遍只认最早那一遍 | 先查在册：在册即把头一遍定局原样回（duplicated），后到的不顶先收的，挂账缘故不补第二句；唯一键 `uk_catalog_book_no` 兜底并发。要改数走差异清单，不在核收记录里追加 | `TMgisCatalogBookServiceImpl.accept` |
| 两个市同日各递一册，与分开两日办一模一样 | 推演无状态、只认本册内容；册号各是各的，落账先后不影响任一册定局（有单测正反两序对算） | `CatalogBookJudge.judge` |
| 同一图幅代号占两行，并一条，幅数只加一次 | 进账行按图幅代号归组，头行 `primary_row_no` 指自己、被并行指头行；幅数合计只加头行。两行仍各算进账行，三数照对 | `CatalogBookJudge.judge`、`CatalogTally` |
| 行数超过上限，整册不收，一行也不落 | 越过 `MAX_ROWS`(2000) 整册顶回，册子与行目一笔不 insert | `CatalogBookJudge.judge` → `CatalogAcceptException` |
| 一张白纸不许回「已核讫」 | 零行（或行目为空）整册顶回，不存在册子、不发回执；册号、属地抬头缺失同办 | `CatalogBookJudge.judge` |
| 翻账两头都要翻得动 | 凭册号、凭属地都能把行目拉出来；挂着几行、进账几行、表上几行当场对（挂着加入账 = 总行数） | `GET /mgisCatalog/byBookNo`、`/byRegion`、`CatalogBookJudge.tally` |
| 回执与库同出一处算 | 接口回话即推演定局；翻账三数不当册子上的死数，自行目经同一个 `tally` 当场重算；少一笔多一笔追得到具体哪一行 | `CatalogBookJudge.tally`、`TMgisCatalogBookServiceImpl.toView` |
| 行次对不上原表整册不收 | 填报行次须为正且册内不撞号；未填的由系统按原表次序补空号。撞号则挂账缘故指不到具体行，整册不收 | `CatalogBookJudge.assignRowNos` |
| 成果类别沿用质检同一套 | 类别取 `qual` 的四档（地形图/影像图/矢量要素/地名注记），代号或中文名皆认；不识照留原值挂账，不擅自归类 | `QualProdType.tryOf` |

## 门脸规矩

- **此门专走「整批提交」**：只有 `POST /mgisCatalog/accept` 一个写入门；册号、属地抬头、全行目一次递齐。
- **一行行拆解入账都压在服务层**：`TMgisCatalogBookServiceImpl` 先推演后落账，控制器不碰章程。
- **整册核成什么局面，但凭系统推演为定**：请求只带填报原值，收不收、挂什么缘故请求里说了不算。
- **接口怎么回、账就怎么记**：回话逐行字段与行目实体一一对落（有单测逐字段对算）。
- **单条添、单条改的门不辟**：两张表只立不改（无 update_by/update_time/del_flag 列，无 add/edit/remove 端点）。
- **名册签名一仍其旧**：`create_by`/`create_time` 仍走全局 MyBatis-Plus 自动填充，登录态是谁就记谁。
- **欲扩充另起新重载**：已发布方法签名不动，新意思加新方法。

## 挂账缘故（`CatalogRejectReason`，按核行次序）

| 代号 | 成文 | 触发 |
| --- | --- | --- |
| `MISSING_SHEET_CODE` | 图幅代号未填 | 代号空白（行次、缘故照写，代号栏空着） |
| `MISSING_PROD_TYPE` | 成果类别未填 | 类别空白 |
| `UNSUPPORTED_PROD_TYPE` | 成果类别不识 | 非四类代号/中文名，原值照留 |
| `MISSING_REGION` | 属地名址未填 | 属地空白 |
| `SHEET_COUNT_NOT_A_NUMBER` | 幅数不是数（填报原值：…） | 空白、写字、带小数、越界整数 |
| `NEGATIVE_SHEET_COUNT` | 幅数不得为负（填报原值：…） | 负数 |

一行犯多条，成文用「；」齐着写全，缘故代号取头一条；幅数类缘故必带填报原值，追得到原笔。

## 三数对账（`CatalogTally`）

| 数 | 口径 |
| --- | --- |
| totalRows 表上总行数 | 原表逐行数得，进账挂账都算 |
| acceptedRows 进账行数 | 解析过得去的行；同代号被并的行仍算进账行 |
| pendingRows 挂账行数 | 有缘故挂着的行 |
| entryCount 进账条目数 | 同图幅代号并成一条后的条数（头行计数） |
| sheetTotal 进账幅数合计 | 只按每条头行的幅数加一次 |

恒等式：`acceptedRows + pendingRows = totalRows`。册子表上另存一份落账时的三数，
翻账时以行目重算为准，两边对不上即库里被人动过。

## 端点

| 端点 | 用途 |
| --- | --- |
| `POST /mgisCatalog/accept` | 整批提交（唯一写入门；权限 `mgisCatalog:accept`）。越线、白表、册号/属地缺失、行次撞号整册顶回；重递 duplicated 为真 |
| `GET  /mgisCatalog/byBookNo` | 凭册号翻账：册子抬头、行目、三数一次拉齐（权限 `mgisCatalog:list`） |
| `GET  /mgisCatalog/byRegion` | 凭属地翻账：该市各册的行目与三数 |

无单条 add/edit/remove 端点；确实要改数，走差异清单，不在核收记录里追加自家话。

建表脚本：`sql/mgis_catalog.sql`（册子、行目两表；行目进账挂账同表，
凭册号、属地、代号都建有索引）。
