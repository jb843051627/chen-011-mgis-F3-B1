package com.fc.v2.model.custom;

import com.fc.v2.common.catalog.CatalogTally;
import com.fc.v2.model.auto.TMgisCatalogBook;
import com.fc.v2.model.auto.TMgisCatalogLine;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 翻账视图：册子抬头、行目、三数对账。凭册号、凭属地翻的都是这一种。
 * 三数不当作册子上的死数回，而是从行目经
 * {@link com.fc.v2.common.catalog.CatalogBookJudge#tally} 当场重算——
 * 回执上的数字与库里的数字出自同一处计算，少一笔多一笔都追得到具体哪一行。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "CatalogBookView", description = "目录册翻账视图")
public class CatalogBookView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 册子抬头（含落账时的三数） */
    @ApiModelProperty(value = "册子抬头")
    private final TMgisCatalogBook book;

    /** 行目（进账、挂账同表，按行次排） */
    @ApiModelProperty(value = "行目")
    private final List<TMgisCatalogLine> lines;

    /** 三数对账（自行目当场重算） */
    @ApiModelProperty(value = "三数对账")
    private final CatalogTally tally;

    public CatalogBookView(TMgisCatalogBook book, List<TMgisCatalogLine> lines, CatalogTally tally) {
        this.book = book;
        this.lines = lines;
        this.tally = tally;
    }

    public TMgisCatalogBook getBook() {
        return book;
    }

    public List<TMgisCatalogLine> getLines() {
        return lines;
    }

    public CatalogTally getTally() {
        return tally;
    }
}
