package com.fc.v2.model.custom;

import com.fc.v2.common.catalog.CatalogVerdict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 整批提交回话。接口怎么回、账就怎么记：回话里的逐行结果与三数，
 * 与落库后凭册号翻出来的同出 {@link com.fc.v2.common.catalog.CatalogBookJudge}
 * 一处推演。同册号送来第二遍时 duplicated 为真，回的是最早那一遍的定局——
 * 只认最早那一遍，挂账缘故也不再补第二句。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "CatalogAcceptResult", description = "目录册整批核收回话")
public class CatalogAcceptResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 册子主键 */
    @ApiModelProperty(value = "册子主键")
    private final Long bookId;

    /** 一册推演定局：逐行结果加三数对账 */
    @ApiModelProperty(value = "逐行结果与三数对账")
    private final CatalogVerdict verdict;

    /** 是否重递（只认最早那一遍，未再落一行） */
    @ApiModelProperty(value = "是否重递")
    private final boolean duplicated;

    public CatalogAcceptResult(Long bookId, CatalogVerdict verdict, boolean duplicated) {
        this.bookId = bookId;
        this.verdict = verdict;
        this.duplicated = duplicated;
    }

    public Long getBookId() {
        return bookId;
    }

    public CatalogVerdict getVerdict() {
        return verdict;
    }

    public boolean isDuplicated() {
        return duplicated;
    }
}
