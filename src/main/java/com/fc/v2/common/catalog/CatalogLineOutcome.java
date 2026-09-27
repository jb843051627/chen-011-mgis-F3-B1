package com.fc.v2.common.catalog;

import java.io.Serializable;

/**
 * 一行的推演结果：行次、图幅代号、成果类别、属地名址四样跟着走，
 * 收下的进账，没收下的挂账——挂账的行次、代号、缘故三样齐着写。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class CatalogLineOutcome implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原表行次（填报所指；未填由系统按序补） */
    private final int rowNo;
    /** 图幅代号（填报原值，去首尾空白） */
    private final String sheetCode;
    /** 成果类别（识得存类别代号，不识照留填报原值） */
    private final String prodType;
    /** 属地名址 */
    private final String region;
    /** 幅数填报原值 */
    private final String sheetCountRaw;
    /** 解析后的幅数（进账行有值；挂账行为空） */
    private final Integer sheetCount;
    /** 归宿：进账 / 挂账 */
    private final CatalogDisposition disposition;
    /** 挂账缘故代号（头一条，按核行次序） */
    private final String reasonCode;
    /** 挂账缘故（成文那一句，命中的缘故齐着写全） */
    private final String reasonText;
    /** 进账组头行行次：同代号并一条，幅数只按头一行加；头行指自己，被并行指头行；挂账行为空 */
    private final Integer primaryRowNo;

    public CatalogLineOutcome(int rowNo, String sheetCode, String prodType, String region,
                              String sheetCountRaw, Integer sheetCount, CatalogDisposition disposition,
                              String reasonCode, String reasonText, Integer primaryRowNo) {
        this.rowNo = rowNo;
        this.sheetCode = sheetCode;
        this.prodType = prodType;
        this.region = region;
        this.sheetCountRaw = sheetCountRaw;
        this.sheetCount = sheetCount;
        this.disposition = disposition;
        this.reasonCode = reasonCode;
        this.reasonText = reasonText;
        this.primaryRowNo = primaryRowNo;
    }

    public int getRowNo() {
        return rowNo;
    }

    public String getSheetCode() {
        return sheetCode;
    }

    public String getProdType() {
        return prodType;
    }

    public String getRegion() {
        return region;
    }

    public String getSheetCountRaw() {
        return sheetCountRaw;
    }

    public Integer getSheetCount() {
        return sheetCount;
    }

    public CatalogDisposition getDisposition() {
        return disposition;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getReasonText() {
        return reasonText;
    }

    public Integer getPrimaryRowNo() {
        return primaryRowNo;
    }

    /** 这一行进没进账 */
    public boolean isAccepted() {
        return disposition == CatalogDisposition.ACCEPTED;
    }

    /** 这一行是不是进账组的头行（幅数只按头一行加一次） */
    public boolean isPrimary() {
        return isAccepted() && primaryRowNo != null && primaryRowNo == rowNo;
    }
}
