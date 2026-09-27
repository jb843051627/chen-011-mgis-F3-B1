package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.qual.QualVerdict;
import com.fc.v2.model.auto.TMgisQualLine;

/**
 * 测绘成果质检判定线 Service接口
 *
 * <p>判定口径（适用性、当日当家、争档、分档、浑数）全部在
 * {@link com.fc.v2.common.qual.QualLineJudge} 一处，本层只负责取数与落线。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
public interface ITMgisQualLineService {

    /** 按主键查询判定线 */
    TMgisQualLine selectTMgisQualLineById(Long id);

    /** 判定线台账（核案台列表页） */
    List<TMgisQualLine> selectTMgisQualLineList(QueryWrapper<TMgisQualLine> queryWrapper);

    /** 录线：代号唯一、类别合法、比例尺档上下界齐全、天花板一≤二≤三、启用早于交棒 */
    int insertTMgisQualLine(TMgisQualLine line);

    /** 改线：校验同录线。改线不动已落账的结论（结论是快照） */
    int updateTMgisQualLine(TMgisQualLine line);

    /** 撤线（逻辑删除，逗号分隔的主键串）。撤下的线不再参与任何判定 */
    int deleteTMgisQualLineByIds(String ids);

    /**
     * 交件日当日管得着这份成果的判定线，按当家规则排好序。
     * 核案台与检验机构自查页面同调此口径，合不上便是事故。
     */
    List<TMgisQualLine> listApplicable(String prodType, Integer scaleDenominator, Date day);

    /**
     * 判定全流程（试算，不落账）：以交件日倒查适用线，无可用线明写当日无可用判定线；
     * 中误差浑数照原样驳回；取当家线分档并给出选定缘由。
     */
    QualVerdict judge(String prodType, Integer scaleDenominator, Date submitDay, String meanErrorRaw);
}
