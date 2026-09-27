package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TMgisQualConclusion;
import com.fc.v2.model.custom.QualIssueRequest;

/**
 * 测绘成果质检判定结论 Service接口
 *
 * <p>结论只立不改：出结论那一刻由系统把判定线的代号与启用之日快照进结论，
 * 事后谁也不许手工补盖——本接口不提供任何改、删入口。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
public interface ITMgisQualConclusionService {

    /**
     * 出结论：以交件日倒查当家判定线，判定通过校验后快照落账。
     * 当日无可用判定线、中误差是浑数，一律驳回，休想进账。
     * 同一幅图今天出与明天出，代号与启用之日两栏一个字不差。
     */
    TMgisQualConclusion issue(QualIssueRequest req);

    /** 按主键查询结论 */
    TMgisQualConclusion selectTMgisQualConclusionById(Long id);

    /** 结论台账（处里核案台） */
    List<TMgisQualConclusion> selectTMgisQualConclusionList(QueryWrapper<TMgisQualConclusion> queryWrapper);

    /** 检验机构自查：按图幅成果代号查结论，与核案台同一张表、同一口径 */
    List<TMgisQualConclusion> selfCheck(String itemCode);
}
