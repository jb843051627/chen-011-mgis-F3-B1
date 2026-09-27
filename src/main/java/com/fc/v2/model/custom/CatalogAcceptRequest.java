package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 整批提交请求。此门专走「整批提交」：一册递进来，册号、属地抬头、
 * 行目一次递齐；每一行只带填报原值（行次、图幅代号、成果类别、属地名址、
 * 幅数），收不收、挂什么缘故，但凭系统一笔笔推演为定，请求里说了不算。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "CatalogAcceptRequest", description = "目录册整批提交请求")
public class CatalogAcceptRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 册号（一册一号；重递只认最早那一遍） */
    @ApiModelProperty(value = "册号", required = true)
    private String bookNo;

    /** 递交属地（十一市册子抬头） */
    @ApiModelProperty(value = "递交属地", required = true)
    private String submitRegion;

    /** 行目（原表逐行，按原表次序递） */
    @ApiModelProperty(value = "行目", required = true)
    private List<Line> lines;

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

    public String getSubmitRegion() {
        return submitRegion;
    }

    public void setSubmitRegion(String submitRegion) {
        this.submitRegion = submitRegion;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    /**
     * 一行填报原值。行次可空：空着由系统按原表次序自一连排；
     * 填了须与原表行次对得上（册内不重，重了整册对不上原表，整册不收）。
     */
    @ApiModel(value = "CatalogAcceptLine", description = "目录册行目填报原值")
    public static class Line implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 原表行次（可空，空则由系统按次序补） */
        @ApiModelProperty(value = "原表行次")
        private Integer rowNo;

        /** 图幅代号 */
        @ApiModelProperty(value = "图幅代号")
        private String sheetCode;

        /** 成果类别（代号或中文名） */
        @ApiModelProperty(value = "成果类别")
        private String prodType;

        /** 属地名址 */
        @ApiModelProperty(value = "属地名址")
        private String region;

        /** 幅数填报原值（照原样递，负数、非数也由系统核） */
        @ApiModelProperty(value = "幅数")
        private String sheetCount;

        public Integer getRowNo() {
            return rowNo;
        }

        public void setRowNo(Integer rowNo) {
            this.rowNo = rowNo;
        }

        public String getSheetCode() {
            return sheetCode;
        }

        public void setSheetCode(String sheetCode) {
            this.sheetCode = sheetCode;
        }

        public String getProdType() {
            return prodType;
        }

        public void setProdType(String prodType) {
            this.prodType = prodType;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getSheetCount() {
            return sheetCount;
        }

        public void setSheetCount(String sheetCount) {
            this.sheetCount = sheetCount;
        }
    }
}
