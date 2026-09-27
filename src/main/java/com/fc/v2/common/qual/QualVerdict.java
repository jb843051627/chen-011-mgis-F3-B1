package com.fc.v2.common.qual;

import com.fc.v2.model.auto.TMgisQualLine;

import java.math.BigDecimal;

/**
 * 判定结果（未落账）：当家的线、档位、压线与否、选定缘由。
 * 落账时由结论服务把线的代号与启用之日一并快照进结论。
 *
 * @author fuce
 * @date 2026-09-26
 */
public final class QualVerdict {

    /** 当家的判定线 */
    private final TMgisQualLine line;

    /** 分档结果 */
    private final QualTier tierResult;

    /** 校验过的中误差（浑数到不了这一步） */
    private final BigDecimal meanError;

    /** 选定缘由：凭什么是这条线 */
    private final String pickReason;

    /** 当日适用线总条数 */
    private final int candidateCount;

    public QualVerdict(TMgisQualLine line, QualTier tierResult, BigDecimal meanError,
                       String pickReason, int candidateCount) {
        this.line = line;
        this.tierResult = tierResult;
        this.meanError = meanError;
        this.pickReason = pickReason;
        this.candidateCount = candidateCount;
    }

    public TMgisQualLine getLine() {
        return line;
    }

    public int getTier() {
        return tierResult.getTier();
    }

    public boolean isOnCeiling() {
        return tierResult.isOnCeiling();
    }

    public QualTier getTierResult() {
        return tierResult;
    }

    public BigDecimal getMeanError() {
        return meanError;
    }

    public String getPickReason() {
        return pickReason;
    }

    public int getCandidateCount() {
        return candidateCount;
    }
}
