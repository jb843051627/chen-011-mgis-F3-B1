package com.fc.v2.common.catalog;

import java.io.Serializable;

/**
 * 三数对账：挂着几行、进账几行、表上一共几行，当场对得上
 * （挂着加入账等于表上总行数）。核收落账与凭册号、凭属地翻账
 * 都经 {@link CatalogBookJudge#tally} 同一处算出——回执上的数字
 * 与库里的数字出自同一处计算，少一笔多一笔都能追到具体哪一行。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class CatalogTally implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 表上总行数 */
    private final int totalRows;
    /** 进账行数（被并掉的重复行仍算进账行） */
    private final int acceptedRows;
    /** 挂账行数 */
    private final int pendingRows;
    /** 进账条目数（同图幅代号并成一条后的条数） */
    private final int entryCount;
    /** 进账幅数合计（同代号只按头一行加一次） */
    private final int sheetTotal;

    public CatalogTally(int totalRows, int acceptedRows, int pendingRows, int entryCount, int sheetTotal) {
        this.totalRows = totalRows;
        this.acceptedRows = acceptedRows;
        this.pendingRows = pendingRows;
        this.entryCount = entryCount;
        this.sheetTotal = sheetTotal;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getAcceptedRows() {
        return acceptedRows;
    }

    public int getPendingRows() {
        return pendingRows;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public int getSheetTotal() {
        return sheetTotal;
    }

    /** 三数当场对：挂着加入账必须等于表上总行数 */
    public boolean balances() {
        return acceptedRows + pendingRows == totalRows;
    }
}
