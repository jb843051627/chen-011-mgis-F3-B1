package com.fc.v2.common.qual;

/**
 * 分档结果：中误差落在哪一档，顺带回一句是否恰好压在天花板上。
 *
 * @author fuce
 * @date 2026-09-26
 */
public final class QualTier {

    /** 档位：1 一档、2 二档、3 三档（三档为顶，越过三档天花板仍归三档） */
    private final int tier;

    /** 是否恰好压在某档天花板上（等号算高一档，但压线一事照实记） */
    private final boolean onCeiling;

    public QualTier(int tier, boolean onCeiling) {
        this.tier = tier;
        this.onCeiling = onCeiling;
    }

    public int getTier() {
        return tier;
    }

    public boolean isOnCeiling() {
        return onCeiling;
    }

    @Override
    public String toString() {
        return "QualTier{tier=" + tier + ", onCeiling=" + onCeiling + '}';
    }
}
