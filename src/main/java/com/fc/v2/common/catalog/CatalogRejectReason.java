package com.fc.v2.common.catalog;

/**
 * 挂账缘故。核行次序即本枚举声明次序：图幅代号、成果类别、属地名址、幅数。
 * 一行可以同时犯好几条，成文时把命中的缘故齐着写全，
 * 代号取头一条落账。
 *
 * <p>一处填错只挂这一行：任何一条都不许连坐整册；整册不收只有
 * 越线、白表两种局面，见 {@link CatalogBookJudge}。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
public enum CatalogRejectReason {

    /** 图幅代号空着：挂账行次、代号、缘故三样要齐，代号空也照实挂 */
    MISSING_SHEET_CODE("图幅代号未填"),
    /** 成果类别空着 */
    MISSING_PROD_TYPE("成果类别未填"),
    /** 成果类别不识（既不是类别代号也不是类别中文名） */
    UNSUPPORTED_PROD_TYPE("成果类别不识"),
    /** 属地名址空着 */
    MISSING_REGION("属地名址未填"),
    /** 幅数根本不是数（空白、写字、带小数等），照原样驳回 */
    SHEET_COUNT_NOT_A_NUMBER("幅数不是数"),
    /** 幅数填成了负数——那一行自己挂着，整册照走 */
    NEGATIVE_SHEET_COUNT("幅数不得为负");

    private final String label;

    CatalogRejectReason(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }
}
