package com.fc.v2.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.qual.QualLineJudge;
import com.fc.v2.common.qual.QualProdType;
import com.fc.v2.common.qual.QualVerdict;
import com.fc.v2.mapper.auto.TMgisQualConclusionMapper;
import com.fc.v2.model.auto.TMgisQualConclusion;
import com.fc.v2.model.auto.TMgisQualLine;
import com.fc.v2.model.custom.QualIssueRequest;
import com.fc.v2.service.ITMgisQualConclusionService;
import com.fc.v2.service.ITMgisQualLineService;

/**
 * 测绘成果质检判定结论 Service业务层处理
 *
 * @author fuce
 * @date 2026-09-26
 */
@Service
public class TMgisQualConclusionServiceImpl implements ITMgisQualConclusionService {

    @javax.annotation.Resource
    private TMgisQualConclusionMapper mgisQualConclusionMapper;

    @javax.annotation.Resource
    private ITMgisQualLineService mgisQualLineService;

    /**
     * 出结论。判定不过（无可用线、浑数）在落账前抛出，一笔都不会写进库里。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMgisQualConclusion issue(QualIssueRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("出结论请求为空");
        }
        if (req.getItemCode() == null || req.getItemCode().trim().isEmpty()) {
            throw new IllegalArgumentException("图幅成果代号未填报");
        }
        // 判定以交件日为准：2019 年交件的图今年来讨结论，看的是交件那天哪条作数
        Date submitDay = QualLineJudge.parseDay(req.getSubmitDate());
        QualVerdict verdict = mgisQualLineService.judge(
                req.getProdType(), req.getScaleDenominator(), submitDay, req.getMeanError());
        TMgisQualLine line = verdict.getLine();

        TMgisQualConclusion conclusion = new TMgisQualConclusion();
        conclusion.setItemCode(req.getItemCode().trim());
        // 批号随结论落账：不合格件夜里归集立整改单时按批归堆；空着的不归集
        conclusion.setBatchNo(req.getBatchNo() == null || req.getBatchNo().trim().isEmpty()
                ? null : req.getBatchNo().trim());
        conclusion.setProdType(QualProdType.require(req.getProdType()).name());
        conclusion.setScaleDenominator(req.getScaleDenominator());
        conclusion.setSubmitDate(submitDay);
        conclusion.setMeanError(verdict.getMeanError());
        // 快照：代号与启用之日落账即定，事后改线不动旧结论，谁也不许手工补盖
        conclusion.setRuleId(line.getId());
        conclusion.setRuleCode(line.getRuleCode());
        conclusion.setRuleName(line.getRuleName());
        conclusion.setRuleEffStart(line.getEffStart());
        conclusion.setTier(verdict.getTier());
        conclusion.setOnCeiling(verdict.isOnCeiling() ? 1 : 0);
        conclusion.setPickReason(verdict.getPickReason());
        conclusion.setIssuedAt(new Date());
        this.mgisQualConclusionMapper.insert(conclusion);
        return conclusion;
    }

    @Override
    public TMgisQualConclusion selectTMgisQualConclusionById(Long id) {
        return this.mgisQualConclusionMapper.selectById(id);
    }

    @Override
    public List<TMgisQualConclusion> selectTMgisQualConclusionList(QueryWrapper<TMgisQualConclusion> queryWrapper) {
        return this.mgisQualConclusionMapper.selectList(queryWrapper);
    }

    @Override
    public List<TMgisQualConclusion> selfCheck(String itemCode) {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("图幅成果代号未填报");
        }
        return this.mgisQualConclusionMapper.selectList(new QueryWrapper<TMgisQualConclusion>()
                .eq("item_code", itemCode.trim()).orderByDesc("issued_at"));
    }
}
