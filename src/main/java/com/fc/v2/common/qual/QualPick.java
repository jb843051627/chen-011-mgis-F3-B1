package com.fc.v2.common.qual;

import com.fc.v2.model.auto.TMgisQualLine;

/**
 * 取线结果：哪一条当家，连同"凭什么是它"一起给出——
 * 被挑中的一条要在结论里说清凭什么是它。
 *
 * @author fuce
 * @date 2026-09-26
 */
public final class QualPick {

    /** 当家的判定线 */
    private final TMgisQualLine line;

    /** 选定缘由（仅此一条 / 顺位值最高 / 顺位并列代号靠后） */
    private final String reason;

    /** 当日适用线总条数 */
    private final int candidateCount;

    public QualPick(TMgisQualLine line, String reason, int candidateCount) {
        this.line = line;
        this.reason = reason;
        this.candidateCount = candidateCount;
    }

    public TMgisQualLine getLine() {
        return line;
    }

    public String getReason() {
        return reason;
    }

    public int getCandidateCount() {
        return candidateCount;
    }
}
