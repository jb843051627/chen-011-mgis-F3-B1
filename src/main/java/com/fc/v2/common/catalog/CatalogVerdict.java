package com.fc.v2.common.catalog;

import java.io.Serializable;
import java.util.List;

/**
 * 一册的推演定局：逐行结果加三数对账。整册核成什么局面，
 * 但凭系统一笔笔推演为定——接口怎么回、账就怎么记，同出这一份。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class CatalogVerdict implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 册号 */
    private final String bookNo;
    /** 递交属地 */
    private final String submitRegion;
    /** 逐行推演结果（按原表行次排） */
    private final List<CatalogLineOutcome> lines;
    /** 三数对账（挂着加入账等于表上总行数） */
    private final CatalogTally tally;

    public CatalogVerdict(String bookNo, String submitRegion, List<CatalogLineOutcome> lines, CatalogTally tally) {
        this.bookNo = bookNo;
        this.submitRegion = submitRegion;
        this.lines = lines;
        this.tally = tally;
    }

    public String getBookNo() {
        return bookNo;
    }

    public String getSubmitRegion() {
        return submitRegion;
    }

    public List<CatalogLineOutcome> getLines() {
        return lines;
    }

    public CatalogTally getTally() {
        return tally;
    }
}
