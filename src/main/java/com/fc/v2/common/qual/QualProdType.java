package com.fc.v2.common.qual;

/**
 * 成果类别：判定线按类别各排各的，互不通用——
 * 不兴拿影像图的线去量地形图。
 *
 * @author fuce
 * @date 2026-09-26
 */
public enum QualProdType {

    /** 地形图 */
    TOPO_MAP("地形图"),
    /** 影像图 */
    IMAGE_MAP("影像图"),
    /** 矢量要素 */
    VECTOR_FEATURE("矢量要素"),
    /** 地名注记 */
    PLACE_NAME("地名注记");

    private final String label;

    QualProdType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 按代号或中文名认领类别；认不得返回 null（调用方按"管不着"处理）。
     */
    public static QualProdType tryOf(String code) {
        if (code == null) {
            return null;
        }
        String s = code.trim();
        for (QualProdType t : values()) {
            if (t.name().equalsIgnoreCase(s) || t.label.equals(s)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 按代号或中文名认领类别；认不得即驳回（成果类别是判定的头一道口径，不许含糊）。
     */
    public static QualProdType require(String code) {
        QualProdType t = tryOf(code);
        if (t == null) {
            throw new IllegalArgumentException("成果类别不识：" + code);
        }
        return t;
    }
}
