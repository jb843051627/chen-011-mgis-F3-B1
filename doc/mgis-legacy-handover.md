# 整批核收与单据流转 修缮交接册

> 2026-09-27 修缮。一册在手，后来人不必从头趟雷。
> 每条毛病三样齐着写：犯在哪、章程怎么说、复现测试压在哪。

## 章程的出处（只认这几处，别处说了不算）

| 模块 | 章程出处 | 落点 |
| --- | --- | --- |
| 年度图幅目录册核收 | `doc/mgis-catalog-book.md` | `CatalogBookJudge`、`TMgisCatalogBookServiceImpl` |
| 质检判定线 / 结论 | `doc/mgis-qual-line.md` | `QualLineJudge`、`TMgisQualLineServiceImpl`、`TMgisQualConclusionServiceImpl` |
| 整改回执单（六站） | `doc/mgis-rectify-line.md` | `RectifyFlow`、`RectifyChainJudge`、`TMgisRectifyOrderServiceImpl` |
| 成果目录汇交核收行（整批） | `ITMgisSubmitRowService` javadoc | `TMgisSubmitRowServiceImpl` |
| 成果提供使用逐级核准单（签批链） | 实体字段注释 + `ITMgisUseBillService` | `TMgisUseBillServiceImpl` |
| 不合格成果整改回执单（关口流转） | 实体字段注释 + `ITMgisCheckCardService` | `TMgisCheckCardServiceImpl` |
| 到期提醒派发（周期任务） | `ITMgisDutyTaskService` javadoc | `TMgisDutyTaskServiceImpl` |
| 作业范围标签建档卡 | `ITMgisScopeCardService` + `doc/schema/mgis.sql` | `TMgisScopeCardServiceImpl` |

## 通病七条（修缮定下的口径，新写代码照此办理）

1. **重递认头一遍**：同一批、同一册、同一点第二回来的，不新落一行，头一遍原样回；唯一键兜底并发。
2. **回执与流水同源**：接口回的数与库里的数出自同一处计算；回 0 而库里有行、回 N 而库里无账，都是事故。
3. **状态拴一处**：层数/关口与情形一次挪齐，不许两处各留各的写法；终态（已核讫、已打回、已销号、已封存）的单子什么动作都不收。
4. **坏珠不连坐**：一处填错只挂那一行（条），合格行照常入账；整批退回只许用于越线、白表、册号缺失这类整册层面的毛病。
5. **两道门走一样的规**：分档、状态、校验只认一处章程；列表页与详情页同一口径，服务层不私设门槛、不另起分页。
6. **空批平稳托住**：空批返 0，不抛错、不落库。
7. **越线整批顶回**：行数超上限返约定值（-1 或整册异常），一行也不入库。

## 毛病台账（逐条对号，复现测试压着）

| # | 毛病 | 犯在哪 | 修法 | 复现测试 |
| --- | --- | --- | --- | --- |
| 1 | 回合一重试，账上长出两份 | `TMgisSubmitRowServiceImpl.submitBatch` 无幂等，重提全量重插；`TMgisDutyTaskServiceImpl` 不滤已处理、跑完不落状态，下轮再派一遍 | 重递认头一遍（先查在账，唯一键 `uk_submit_row_batch_row` 兜底）；派发只出「待提醒」，跑过即落「已提醒/送不出去」 | `legacy/TMgisSubmitRowServiceImplTest#同一批重复提交不重复入库`、`legacy/TMgisDutyTaskServiceImplTest#已处理的条目不再重复派发` |
| 2 | 单据挪状态两条通道各执一词 | `TMgisUseBillServiceImpl`：approve 挪状态、reject 什么都不落；`TMgisCheckCardServiceImpl`：advance 一步跨两档、status 常数与实体注释对不上 | 三条通道同一套状态章程：否决落「已打回」；推进一档就是一档，关口与落定拴一处 | `legacy/TMgisUseBillServiceImplTest`、`legacy/TMgisCheckCardServiceImplTest` |
| 3 | 行数超线全数入库 | `TMgisSubmitRowServiceImpl`：`MAX_ROWS=500` 立了从不校验 | 越线返 -1，整批一行不入库 | `legacy/TMgisSubmitRowServiceImplTest#行数超过单批上限整批不入库返负一` |
| 4 | 回执数与流水数各一说 | `TMgisSubmitRowServiceImpl`：有坏行返 0 而库里落了坏行；`TMgisDutyTaskServiceImpl.runOnce` 回成功数却一笔不落 | 成功行数 = 库里入账行数；跑过的逐条落状态 | `legacy/TMgisSubmitRowServiceImplTest#返回成功行数与库里入账行数一致`、`legacy/TMgisDutyTaskServiceImplTest#跑过的条目标已提醒回执与落账一致` |
| 5 | 失败行号齐齐错位 | `TMgisSubmitRowServiceImpl`：失败行按失败序列重排 1,2,3…；`CatalogBookJudge.assignRowNos`：填报号在前时未填行倒填小号，与原表次序脱节 | 行次即原表序（自 1 起）；补号顺原表次序单调排，填报号与原表相悖整册不收 | `legacy/TMgisSubmitRowServiceImplTest#失败行明细保留原始行次不重排`、`catalog/CatalogRowNoAssignTest` |
| 6 | 超时回滚两处状态各留各的 | `TMgisUseBillServiceImpl.rollback`：只退层数不退情形（已核讫回滚后层 1 仍挂「已核讫」）；`TMgisCheckCardServiceImpl.rollback`：一脚踹回起点、落定不动 | 回退时层数/关口与情形一次挪齐回「在核/在办」 | `legacy/TMgisUseBillServiceImplTest#已核讫回退状态一并回在核`、`legacy/TMgisCheckCardServiceImplTest#已销号回退状态一并回在办` |
| 7 | 空批抛错收场 | `TMgisSubmitRowServiceImpl`：`rows.get(0)` 空批炸；`TMgisDutyTaskServiceImpl.runOnce`：`due.get(0)` 空批炸 | 空批平稳返 0，不抛错、不落库 | `legacy/TMgisSubmitRowServiceImplTest#空批次平稳托住返回零不落库`、`legacy/TMgisDutyTaskServiceImplTest#没有到期条目平稳收零` |
| 8 | 列表页与详情页两样数 | `TMgisScopeCardServiceImpl.selectTMgisScopeCardList`：撇开调用方条件，私设 `status=0` 门槛 + 硬编码 `startPage(1,10)` | 按调用方条件查，分页归调用方，服务层只补未删除底线 | `legacy/TMgisScopeCardServiceImplTest#列表与详情同一口径_按调用方条件查` |
| 9 | 一提交里重样明细条条进账 | 整批核收无幂等时，重样即重递的另一种脸 | 同批重递只认头一遍；册内同代号并一条（目录册口径，`CatalogBookJudge` 原有） | 同 #1；目录册侧 `catalog/CatalogBookJudgeTest#同图幅代号占两行并成一条幅数只加一次` |
| 10 | 两道门走一样的规，别只钉一头 | `TMgisScopeCardServiceImpl` 分档：等号算低一档、还设四档（判定线章程：等号算高一档、不设四档）；`TMgisRectifyOrderServiceImpl`：退回重办重试被顶回，别站重试都认头一笔 | 分档与判定线同规（达到上限按高一档折算，越过三档仍归三档）；退回重试认链尾带来当前轮的那一笔 | `legacy/TMgisScopeCardServiceImplTest#达到上限按高一档折算`、`#越过三档天花板仍归三档不设四档`、`rectify/RectifySendBackRetryTest` |
| 11 | 一两行坏珠拖累全串 | `TMgisSubmitRowServiceImpl`：有坏行全批返 0、合格行一条不入；`TMgisDutyTaskServiceImpl.runOnce`：单条空指针全批炸 | 坏行只挂自己；单条失败标「送不出去」跳过继续 | `legacy/TMgisSubmitRowServiceImplTest#坏行不连坐合格行照常入账`、`legacy/TMgisDutyTaskServiceImplTest#单条失败跳过继续不连坐` |
| 12 | 几道门联动的账没拴一处 | `TMgisSubmitRowServiceImpl`/`TMgisUseBillServiceImpl`/`TMgisCheckCardServiceImpl` 多行写库无事务 | 多行写库的方法挂 `@Transactional(rollbackFor = Exception.class)`；派发逐条独立落账（单条失败要跳得过，故不裹大事务） | 见各服务测试；事务性由实现类注解兜住 |

## 顺手拾的小漏

- `TMgisScopeCardServiceImpl.insertTMgisScopeCard` 曾把 `create_by` 写成卡号——名册签名归全局自动填充，服务层不抢这笔。
- `TMgisScopeCardServiceImpl` 分档曾读 `th3_max` 不设防空——档案上限缺维护时按 0 档落，不抛空指针。
- `TMgisDutyTaskServiceImpl` 的时刻比较由墙钟字符串改为毫秒值——字符串比法在时区不对称时把「恰好到期」整体带偏。
- `doc/schema/mgis.sql`：`t_mgis_submit_row` 补唯一键 `uk_submit_row_batch_row (batch_no, row_no)`——并发同递一批由库兜底，老库需补同名索引。

## 行为变更知会（老调用方对一遍）

- `submitBatch`：空批由抛错改为返 0；越线由全量入库改为返 -1 整批不入；有坏行由全批返 0 改为合格入账、返回成功行数；重递由重复入库改为认头一遍。
- `approve/reject/rollback`（核准单）：终态（已核讫、已打回）再操作由「照办」改为拒（返 null）；否决由「不落状态」改为落「已打回」；回退由「只退层数」改为层数与情形一起回。
- `advance/rollback`（回执单）：推进由一步两档改为一档；末关（第 5 关）落「已销号」；已办结不再推进、不得改内容、不得删除；回退由归零改为退一档。
- `runOnce`（派发）：空批由抛错改为返 0；跑过的条目落「已提醒/送不出去」，不再重复派。
- `selectTMgisScopeCardList`：不再私设 `status=0` 与分页，按调用方条件查（分页归调用方）。
- `CatalogBookJudge` 行次归置：填报行次与原表先后相悖、或未填行按原表次序无空号可补的，由「错位照收」改为整册不收。

## 验证

`mvn test` 全绿：119 个测试（87 个原有 + 32 个本次新增/更正）。
新增复现测试先红后绿——红即毛病复现，绿即修缮到位。
