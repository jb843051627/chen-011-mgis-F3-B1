# 整改回执单规矩 → 系统口径对照

质量监督处定下的走站规矩，在系统里只有一处出处：`com.fc.v2.common.rectify.RectifyFlow`（走站）
与 `com.fc.v2.common.rectify.RectifyChainJudge`（拉链）。承办门、零点归集任务、页面、链校验
全从这两处过——两处口径合不上便是事故，所以别处不许另写一套。

一批成果从挂出问题到销号，全程只凭系统里那一张整改回执单，一单对一批，
单上分六站：挂号、告知、整改、复测、销号、封存。

| 处里的说法 | 系统口径 | 落点 |
| --- | --- | --- |
| 一单对一批 | 批号 + 在途标记联合唯一：在途第二张立不出来；封存时在途标记置 NULL，重查才立得起新单 | `t_mgis_rectify_order` `uk_rectify_batch_inflight` |
| 每天零点归集前一日检出的件立单 | 三档结论即不合格件；零点任务按批归集立单，挂号那一笔由系统落笔（经办「系统归集」） | `MgisRectifyTask.raiseDailyOrders` → `TMgisRectifyOrderServiceImpl.raiseDailyOrders` |
| 起单、存单二门焊死 | 控制器没有立单、改单、删单端点；立单只由零点任务起，落笔只经承办门 | `MgisRectifyController` |
| 门面只留「承办」一扇 | 告知/整改/复测/退回/销号/封存全打 `POST /mgisRectify/undertake` 进；一站站地挪、退回一站，章程都在服务层 | `MgisRectifyController.undertake` → `TMgisRectifyOrderServiceImpl.undertake` |
| 承办到没到底，页面不许先落一笔；生效与否只认接口回话 | 经办自登录态取、落笔时辰由服务层写死，请求里说了不算；回话里是落了的那一笔与单子眼下的站，页面只按回话刷新 | `MgisRectifyController.undertake`、`TMgisRectifyOrderServiceImpl.undertake` |
| 只许一站接一站 | 单子停在哪一站就只开哪一站的口子；隔着站落笔当场顶回，不生效 | `RectifyFlow.checkStation` |
| 同一点第二回来的只认头一笔 | 同站同动作同轮已有落笔：不再落笔，头一笔原样回给它（哪怕单子已挪站）；唯一键 `(order_id, station, action, round_no)` 兜底并发 | `TMgisRectifyOrderServiceImpl.undertake`、`uk_rectify_entry_point` |
| 经办、时辰落笔那一下系统写死 | `penned_by`/`penned_at` 由服务层在落笔时写（时辰以秒计，与库面 DATETIME 同口径）；落笔链表不设 update/del_flag 列，服务层无改删入口——事后由不得人回头补录，也不许挪时辰 | `t_mgis_rectify_entry` |
| 告知三样落笔即定 | 问题图幅、抽样口径、复测凭证，三样缺一样落不了笔；落笔同时快照进单（`notify_*`），往后的站只读取不重填 | `RectifyFlow.packPayload`、`TMgisRectifyOrderServiceImpl.moveStation` |
| 整改站想改抽样口径，当场顶回 | 整改那一笔夹带三样中任何一样，直接顶回，一笔都不写 | `RectifyFlow.packPayload` |
| 改出来的说法不许盖掉先前落笔那一笔 | 落笔链只增不改；退回重办由新一笔指向被顶替的那一笔（`supersedes_entry_id`），旧笔不删不改 | `t_mgis_rectify_entry` |
| 复测认两个人 | 检验机构（RETEST_AGENCY）、属地市局（RETEST_CITY）两名的复核意见都要进单；只回来一名，单子仍停在复测 | `RectifyFlow.stationAfter` |
| 复测看出合不上，把整改退回重办 | 退回只许从复测退回整改（SEND_BACK），轮次加一；退回缘由必填 | `RectifyFlow.stationAfter`、`RectifyFlow.isSendBack` |
| 重办前那一站原先写的话退出页面 | 页面每一站只显最新一轮落笔的话，链上两代人的笔都还在——不并排挂着，来检查的人不用问哪一份算数 | `RectifyFlow.pageOf` |
| 销号往下即封存 | 封存落笔后单子状态置封存、在途标记置 NULL；封了存的单子不再挂新条目，也不许把哪一站整条摘除（无改删端点）；要重查另行立单，从挂号重新走满六站，两张各算各的 | `RectifyFlow.checkWritable`、`TMgisRectifyOrderServiceImpl.moveStation` |
| 顺拉倒拉验链 | 每笔记前一笔哈希（头一笔认 64 个 0），哈希押进站、动作、写的话、经办、时辰；顺拉自挂号念到封存，逐笔重算、逐笔对接，须与当日逐站页面上留下的内容一字不差；倒拉自封存沿前哈希念回挂号，步数须与总笔数相合。少一笔、次序错一处、时辰倒挂、内容被改动，这批整改即不合格 | `RectifyChainJudge.verifyForward` / `verifyBackward` |
| 旧签名照原样封存，添新功能请新起重载 | 已发布的方法签名一律不动；结论表加批号走的是加列加字段，`issue(QualIssueRequest)` 签名原样 | `ITMgisRectifyOrderService` javadoc、`TMgisQualConclusion.batchNo` |

## 落笔动作（`RectifyAction`）

| 动作 | 落在哪一站 | 这一笔要说的话 |
| --- | --- | --- |
| REGISTER 挂号 | 挂号 | 零点归集系统落笔（检出日、批号、件数），承办门不收 |
| NOTIFY 告知 | 告知 | 三样：问题图幅 maps、抽样口径 sampling、复测凭证 evidence（必填） |
| RECTIFY 整改 | 整改 | 整改说明 note（必填）；夹带告知三样即顶回 |
| RETEST_AGENCY 复测·检验机构 | 复测 | 复核意见 opinion（必填）、实抽幅数 sampledCount（可空） |
| RETEST_CITY 复测·属地市局 | 复测 | 同上；两名都进单这一站才算走通 |
| SEND_BACK 退回重办 | 复测 | 退回缘由 reason（必填）；只许退回复测前那一站 |
| CLOSE 销号 | 销号 | 附言 note（可空） |
| ARCHIVE 封存 | 封存 | 附言 note（可空）；落笔后单子定案 |

## 端点

| 端点 | 用途 |
| --- | --- |
| `POST /mgisRectify/undertake` | 承办（唯一动作门；权限 `mgisRectify:undertake`） |
| `GET  /mgisRectify/list` | 回执单台账（按批号、站、状态查） |
| `GET  /mgisRectify/{id}` | 回执单页面：只开眼下那一站的口子，每站只显最新一轮 |
| `GET  /mgisRectify/{id}/chain` | 落笔链校验：顺拉倒拉，两程皆通才算合格 |

定时任务：`mgisRectifyTask.raiseDailyOrders()`，cron `0 0 0 * * ?`（每日零点归集前一日检出的件）。

建表脚本：`sql/mgis_rectify.sql`（回执单、落笔链、归集件三表；另含结论表补批号的补列语句，
新库按 `sql/mgis_qual.sql` 建表已带出 `batch_no`）。
