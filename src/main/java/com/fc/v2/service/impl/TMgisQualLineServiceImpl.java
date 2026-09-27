package com.fc.v2.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.qual.QualLineJudge;
import com.fc.v2.common.qual.QualProdType;
import com.fc.v2.common.qual.QualVerdict;
import com.fc.v2.mapper.auto.TMgisQualLineMapper;
import com.fc.v2.model.auto.TMgisQualLine;
import com.fc.v2.service.ITMgisQualLineService;

/**
 * 测绘成果质检判定线 Service业务层处理
 *
 * <p>判定口径只在 {@link QualLineJudge} 一处；本层负责取未删除的线与录线校验。
 * 线的"当家/让位"一律以启用之日、交棒之日为准倒查，status 仅作台账展示，
 * 不参与判定——否则 2019 年交件的图今年来讨结论时，老线会被误挡在门外。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@Service
public class TMgisQualLineServiceImpl implements ITMgisQualLineService {

    @javax.annotation.Resource
    private TMgisQualLineMapper mgisQualLineMapper;

    @Override
    public TMgisQualLine selectTMgisQualLineById(Long id) {
        return this.mgisQualLineMapper.selectById(id);
    }

    @Override
    public List<TMgisQualLine> selectTMgisQualLineList(QueryWrapper<TMgisQualLine> queryWrapper) {
        return this.mgisQualLineMapper.selectList(queryWrapper);
    }

    @Override
    public int insertTMgisQualLine(TMgisQualLine line) {
        validateLine(line);
        checkCodeUnique(line.getRuleCode(), null);
        // 归一落库：类别存枚举代号，代号、名称去首尾空白
        line.setProdType(QualProdType.require(line.getProdType()).name());
        line.setRuleCode(line.getRuleCode().trim());
        line.setRuleName(line.getRuleName().trim());
        if (line.getDelFlag() == null) {
            line.setDelFlag(0);
        }
        if (line.getStatus() == null) {
            line.setStatus(0);
        }
        return this.mgisQualLineMapper.insert(line);
    }

    @Override
    public int updateTMgisQualLine(TMgisQualLine line) {
        if (line == null || line.getId() == null) {
            throw new IllegalArgumentException("改线缺主键");
        }
        validateLine(line);
        checkCodeUnique(line.getRuleCode(), line.getId());
        // 改线按整行说话：可空栏（比例尺档、三档天花板、交棒之日）也要能清空，
        // 故逐栏显式 set，不走 updateById 的空值跳过
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TMgisQualLine> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TMgisQualLine>();
        uw.eq("id", line.getId())
                .set("rule_code", line.getRuleCode().trim())
                .set("rule_name", line.getRuleName().trim())
                .set("prod_type", QualProdType.require(line.getProdType()).name())
                .set("scale_min", line.getScaleMin())
                .set("scale_max", line.getScaleMax())
                .set("th1_max", line.getTh1Max())
                .set("th2_max", line.getTh2Max())
                .set("th3_max", line.getTh3Max())
                .set("eff_start", line.getEffStart())
                .set("eff_end", line.getEffEnd())
                .set("priority", line.getPriority())
                .set("status", line.getStatus())
                .set("remark", line.getRemark());
        return this.mgisQualLineMapper.update(null, uw);
    }

    @Override
    public int deleteTMgisQualLineByIds(String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            throw new IllegalArgumentException("撤线缺主键");
        }
        int rows = 0;
        for (String s : ids.split(",")) {
            TMgisQualLine dead = new TMgisQualLine();
            dead.setId(Long.valueOf(s.trim()));
            dead.setDelFlag(1);
            rows += this.mgisQualLineMapper.updateById(dead);
        }
        return rows;
    }

    @Override
    public List<TMgisQualLine> listApplicable(String prodType, Integer scaleDenominator, Date day) {
        QualProdType type = QualProdType.require(prodType);
        if (scaleDenominator == null || scaleDenominator <= 0) {
            throw new IllegalArgumentException("比例尺分母不正：" + scaleDenominator);
        }
        if (day == null) {
            throw new IllegalArgumentException("交件日期未填报");
        }
        List<TMgisQualLine> all = listAlive();
        return QualLineJudge.applicable(all, type.name(), scaleDenominator, day);
    }

    @Override
    public QualVerdict judge(String prodType, Integer scaleDenominator, Date submitDay, String meanErrorRaw) {
        return QualLineJudge.judge(listAlive(), prodType, scaleDenominator, submitDay, meanErrorRaw);
    }

    /** 未删除的全部判定线（判定窗口由引擎按交件日筛，此处不按状态预筛） */
    private List<TMgisQualLine> listAlive() {
        return this.mgisQualLineMapper.selectList(new QueryWrapper<TMgisQualLine>().eq("del_flag", 0));
    }

    /**
     * 录线校验：代号、名称、类别、启用之日、顺位值必填；
     * 比例尺档上下界要么都填要么都空（空着不当通吃）；
     * 一、二档天花板必填，三档可留白（留白认作无顶），有填则一≤二≤三；
     * 交棒之日可空，填了就必须晚于启用之日。
     */
    private void validateLine(TMgisQualLine line) {
        if (line == null) {
            throw new IllegalArgumentException("判定线为空");
        }
        if (line.getRuleCode() == null || line.getRuleCode().trim().isEmpty()) {
            throw new IllegalArgumentException("判定线代号未填");
        }
        if (line.getRuleName() == null || line.getRuleName().trim().isEmpty()) {
            throw new IllegalArgumentException("判定线名称未填");
        }
        // 类别认不得即驳回：不兴拿影像图的线去量地形图
        QualProdType.require(line.getProdType());
        if (line.getEffStart() == null) {
            throw new IllegalArgumentException("启用之日未填");
        }
        if (line.getPriority() == null) {
            throw new IllegalArgumentException("争档顺位值未填");
        }
        boolean noMin = line.getScaleMin() == null;
        boolean noMax = line.getScaleMax() == null;
        if (noMin != noMax) {
            throw new IllegalArgumentException("比例尺档上下界须同填同空");
        }
        if (!noMin) {
            if (line.getScaleMin() <= 0 || line.getScaleMax() <= 0) {
                throw new IllegalArgumentException("比例尺档分母须为正数");
            }
            if (line.getScaleMin() > line.getScaleMax()) {
                throw new IllegalArgumentException("比例尺档下界不得大于上界");
            }
        }
        if (line.getTh1Max() == null || line.getTh2Max() == null) {
            throw new IllegalArgumentException("一、二档天花板必填（三档可留白，留白认作无顶）");
        }
        if (line.getTh1Max().signum() <= 0 || line.getTh2Max().signum() <= 0) {
            throw new IllegalArgumentException("天花板须为正数");
        }
        if (line.getTh1Max().compareTo(line.getTh2Max()) > 0) {
            throw new IllegalArgumentException("一档天花板不得高于二档");
        }
        if (line.getTh3Max() != null) {
            if (line.getTh3Max().signum() <= 0) {
                throw new IllegalArgumentException("三档天花板须为正数");
            }
            if (line.getTh2Max().compareTo(line.getTh3Max()) > 0) {
                throw new IllegalArgumentException("二档天花板不得高于三档");
            }
        }
        if (line.getEffEnd() != null && !line.getEffStart().before(line.getEffEnd())) {
            throw new IllegalArgumentException("交棒之日须晚于启用之日");
        }
    }

    /** 代号在有效线里独一份（改线时排除自身） */
    private void checkCodeUnique(String ruleCode, Long excludeId) {
        QueryWrapper<TMgisQualLine> qw = new QueryWrapper<TMgisQualLine>()
                .eq("del_flag", 0).eq("rule_code", ruleCode.trim());
        if (excludeId != null) {
            qw.ne("id", excludeId);
        }
        Integer n = this.mgisQualLineMapper.selectCount(qw);
        if (n != null && n > 0) {
            throw new IllegalArgumentException("判定线代号已存在：" + ruleCode);
        }
    }
}
