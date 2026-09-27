package com.fc.v2.common.catalog;

/**
 * 一行的归宿：进账、挂账两种。
 * 挂着加入账等于表上总行数——没有第三种去处。
 *
 * @author fuce
 * @date 2026-09-26
 */
public enum CatalogDisposition {

    /** 进账：解析过得去，已归并进账条目 */
    ACCEPTED("进账"),
    /** 挂账：本行有缘故，自己挂着，不连坐别行 */
    PENDING("挂账");

    private final String label;

    CatalogDisposition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
