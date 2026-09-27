package com.fc.v2.common.rectify;

/**
 * 整链校验回话：顺拉一程、倒拉一程，两程都通这批整改才算合格。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class RectifyChainReport {

    /** 链上总笔数 */
    private final int entryCount;

    /** 顺拉通不通（从挂号念到封存） */
    private final boolean forwardOk;

    /** 倒拉通不通（从封存念回挂号） */
    private final boolean backwardOk;

    /** 断在第几笔，不断为 null */
    private final Long breakSeq;

    /** 回话 */
    private final String message;

    public RectifyChainReport(int entryCount, RectifyChainJudge.Leg forward, RectifyChainJudge.Leg backward) {
        this.entryCount = entryCount;
        this.forwardOk = forward.isOk();
        this.backwardOk = backward.isOk();
        this.breakSeq = forward.getBreakSeq() != null ? forward.getBreakSeq() : backward.getBreakSeq();
        if (this.forwardOk && this.backwardOk) {
            this.message = "链完整：顺拉倒拉皆通，共 " + entryCount + " 笔，这批整改合格";
        } else {
            this.message = "顺拉：" + forward.getMessage() + "；倒拉：" + backward.getMessage()
                    + "——这批整改不合格";
        }
    }

    public int getEntryCount() {
        return entryCount;
    }

    public boolean isForwardOk() {
        return forwardOk;
    }

    public boolean isBackwardOk() {
        return backwardOk;
    }

    /** 两程都通才算合格 */
    public boolean isOk() {
        return forwardOk && backwardOk;
    }

    public Long getBreakSeq() {
        return breakSeq;
    }

    public String getMessage() {
        return message;
    }
}
