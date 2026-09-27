package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 出结论请求。中误差收原样字符串——浑数要照原样驳回，不许先过一道数字转换。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "QualIssueRequest", description = "出结论请求")
public class QualIssueRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 图幅成果代号 */
    @ApiModelProperty(value = "图幅成果代号", required = true)
    private String itemCode;

    /** 成果批号（可空；填了则不合格件零点归集时按批立整改回执单） */
    @ApiModelProperty(value = "成果批号")
    private String batchNo;

    /** 成果类别 TOPO_MAP地形图 IMAGE_MAP影像图 VECTOR_FEATURE矢量要素 PLACE_NAME地名注记（中文名亦可） */
    @ApiModelProperty(value = "成果类别", required = true)
    private String prodType;

    /** 比例尺分母(如 1:2000 传 2000) */
    @ApiModelProperty(value = "比例尺分母", required = true)
    private Integer scaleDenominator;

    /** 交件日期 yyyy-MM-dd（判定以交件日为准） */
    @ApiModelProperty(value = "交件日期 yyyy-MM-dd", required = true)
    private String submitDate;

    /** 中误差填报原值(米) */
    @ApiModelProperty(value = "中误差填报原值(米)", required = true)
    private String meanError;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getProdType() {
        return prodType;
    }

    public void setProdType(String prodType) {
        this.prodType = prodType;
    }

    public Integer getScaleDenominator() {
        return scaleDenominator;
    }

    public void setScaleDenominator(Integer scaleDenominator) {
        this.scaleDenominator = scaleDenominator;
    }

    public String getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(String submitDate) {
        this.submitDate = submitDate;
    }

    public String getMeanError() {
        return meanError;
    }

    public void setMeanError(String meanError) {
        this.meanError = meanError;
    }
}
