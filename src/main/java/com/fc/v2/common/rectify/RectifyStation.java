package com.fc.v2.common.rectify;

/**
 * 整改回执单六站：挂号、告知、整改、复测、销号、封存。
 * 走站只许一站接一站，次序写死在这里，别处不许另排。
 *
 * @author fuce
 * @date 2026-09-26
 */
public enum RectifyStation {

    /** 挂号：零点归集立单，系统落笔 */
    REGISTER("挂号", 0),
    /** 告知：说定三样——问题图幅、抽样口径、复测凭证；落笔即定，往后只读 */
    NOTIFY("告知", 1),
    /** 整改：针对告知的问题图幅整改；不许动告知的三样 */
    RECTIFY("整改", 2),
    /** 复测：检验机构一名、属地市局一名，两名复核意见都要进单 */
    RETEST("复测", 3),
    /** 销号 */
    CLOSE("销号", 4),
    /** 封存：封了存的单子不再挂新条目，也不许摘除 */
    ARCHIVE("封存", 5);

    private final String label;
    private final int order;

    RectifyStation(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    /** 站次（挂号为 0，依次加一），只许比邻走站 */
    public int getOrder() {
        return order;
    }

    /** 下一站；封存已是末站，再往下仍归封存 */
    public RectifyStation next() {
        int n = ordinal() + 1;
        return n >= values().length ? ARCHIVE : values()[n];
    }

    /**
     * 按代号或中文站名认领；认不得即驳回。
     */
    public static RectifyStation require(String code) {
        if (code != null) {
            String s = code.trim();
            for (RectifyStation st : values()) {
                if (st.name().equalsIgnoreCase(s) || st.label.equals(s)) {
                    return st;
                }
            }
        }
        throw new IllegalArgumentException("站名不识：" + code);
    }
}
