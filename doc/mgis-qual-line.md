# 判定线规矩 → 系统口径对照

质量监督处定下的规矩，在系统里只有一处出处：`com.fc.v2.common.qual.QualLineJudge`。
核案台（`/mgisQualConclusion/list`）、检验机构自查页面（`/mgisQualConclusion/selfCheck`）、
出结论工序（`/mgisQualConclusion/issue`）全从这一处过——两处口径合不上便是事故，
所以别处不许另写一套判定逻辑。

| 处里的说法 | 系统口径 | 落点 |
| --- | --- | --- |
| 结论来路要明 | 出结论那一刻把判定线**代号**与**启用之日**快照进结论；同一幅图今天出明天出，这两栏一个字不差 | `TMgisQualConclusionServiceImpl.issue` |
| 事后不许手工补盖 | 结论表只立不改：无 update/del_flag 列，服务层与控制器均无改、删入口；判定依据存快照，事后改线不动旧结论 | `t_mgis_qual_conclusion` |
| 管不管得着 | 成果类别不合、比例尺档落在范围之外 → 当没这条线；适用档空着（上下界缺一）→ 不当通吃，什么也管不着 | `QualLineJudge.covers` |
| 以交件日倒查 | 判定窗口看**交件日**，不看出结论之日；2019 年交件的图今年讨结论，用 2019 年当家的线 | `QualLineJudge.inOffice` |
| 交棒当日归属 | 窗口为 **[启用之日, 交棒之日)**：启用当日即管，交棒当日即归新线，旧线管到前一日；交棒之日空 = 现行 | `QualLineJudge.inOffice` |
| 前头日子的不许顶上 | 当日无适用线 → 明写「当日无可用判定线」，不回落、不顶包 | `QualLineJudge.judge` → `NoQualLineException` |
| 等号算高一档 | 中误差恰为一档上限按二档说话，恰为二档上限按三档说话；压线一事照实记（onCeiling） | `QualLineJudge.tierOf` |
| 越过三档天花板 | 归三档，不设四档 | `QualLineJudge.tierOf` |
| 三档留白 | 认作无顶（大误差仍归三档），不许解成断了档没人管 | `QualLineJudge.tierOf` |
| 浑数驳回 | 负数、整数部分一连六个九及以上、不是数 → 照原样驳回（异常带填报原值），休想进账 | `QualLineJudge.parseMeanError` → `QualInputException` |
| 争档 | 先比顺位值，大者优先；顺位相同认代号靠后（字典序大）那条；结论里写清凭什么是它 | `QualLineJudge.pick` |
| 线的当家/让位 | 一律以启用/交棒之日倒查为准；`status` 只作台账展示，不参与判定 | `TMgisQualLineServiceImpl` |

## 录线校验（`TMgisQualLineServiceImpl.validateLine`）

- 代号（有效线内唯一）、名称、成果类别、启用之日、顺位值必填；
- 比例尺档上下界同填同空，填则 0 < 下界 ≤ 上界；
- 一、二档天花板必填，三档可留白；有填则 0 < 一档 ≤ 二档 ≤ 三档；
- 交棒之日可空，填则须晚于启用之日。

## 端点

| 端点 | 用途 |
| --- | --- |
| `GET  /mgisQualLine/list` | 判定线台账 |
| `POST /mgisQualLine/add` / `edit` / `remove` | 录线 / 改线 / 撤线（逻辑删除） |
| `GET  /mgisQualLine/applicable` | 交件日当日适用线（核案台与自查同一口） |
| `POST /mgisQualLine/judge` | 试算，不落账 |
| `POST /mgisQualConclusion/issue` | 出结论（快照落账） |
| `GET  /mgisQualConclusion/list` | 处里核案台 |
| `GET  /mgisQualConclusion/selfCheck` | 检验机构自查 |
| `GET  /mgisQualConclusion/{id}` | 结论详情 |

建表与补列脚本：`sql/mgis_qual.sql`。
