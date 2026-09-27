package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 年度图幅目录册行目对象 t_mgis_catalog_line。
 *
 * <p>原表每一行落一条，进账、挂账同在这一张表：挂着加入账当场对得上
 * 总行数。同图幅代号并成一条时，被并的行 {@code primaryRowNo} 指向
 * 头行行次，幅数只按头一行加一次。只立不改：不设 update/del_flag 列，
 * 服务层无单条添改、删除入口。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_mgis_catalog_line")
@ApiModel(value = "TMgisCatalogLine", description = "年度图幅目录册行目")
public class TMgisCatalogLine implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 册子主键 */
    @TableField("book_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "册子主键")
    private Long bookId;

    /** 册号(冗余,凭册号直接翻行目) */
    @TableField("book_no")
    @ApiModelProperty(value = "册号")
    private String bookNo;

    /** 原表行次(填报所指;未填由系统按序补) */
    @TableField("row_no")
    @ApiModelProperty(value = "原表行次")
    private Integer rowNo;

    /** 图幅代号(挂账行照留填报原值,空缺即为空) */
    @TableField("sheet_code")
    @ApiModelProperty(value = "图幅代号")
    private String sheetCode;

    /** 成果类别(识得存类别代号,不识照留原值) */
    @TableField("prod_type")
    @ApiModelProperty(value = "成果类别")
    private String prodType;

    /** 属地名址 */
    @TableField("region")
    @ApiModelProperty(value = "属地名址")
    private String region;

    /** 幅数填报原值(照原样留,负数/非数也追得到原笔) */
    @TableField("sheet_count_raw")
    @ApiModelProperty(value = "幅数填报原值")
    private String sheetCountRaw;

    /** 解析后的幅数(进账行有值;挂账行为空) */
    @TableField("sheet_count")
    @ApiModelProperty(value = "解析后的幅数")
    private Integer sheetCount;

    /** 归宿 ACCEPTED进账 PENDING挂账 */
    @TableField("disposition")
    @ApiModelProperty(value = "归宿 ACCEPTED进账 PENDING挂账")
    private String disposition;

    /** 挂账缘故代号 */
    @TableField("reason_code")
    @ApiModelProperty(value = "挂账缘故代号")
    private String reasonCode;

    /** 挂账缘故(成文那一句,重递不再补第二句) */
    @TableField("reason_text")
    @ApiModelProperty(value = "挂账缘故")
    private String reasonText;

    /** 进账组头行行次:同代号并一条,幅数只按头一行加;头行指自己,被并行指头行;挂账行为空 */
    @TableField("primary_row_no")
    @ApiModelProperty(value = "进账组头行行次")
    private Integer primaryRowNo;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

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

    public String getSheetCountRaw() {
        return sheetCountRaw;
    }

    public void setSheetCountRaw(String sheetCountRaw) {
        this.sheetCountRaw = sheetCountRaw;
    }

    public Integer getSheetCount() {
        return sheetCount;
    }

    public void setSheetCount(Integer sheetCount) {
        this.sheetCount = sheetCount;
    }

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String string) {
        this.reasonCode = string;
    }

    public String getReasonText() {
        return reasonText;
    }

    public void setReasonText(String reasonText) {
        this.reasonText = reasonText;
    }

    public Integer getPrimaryRowNo() {
        return primaryRowNo;
    }

    public void setPrimaryRowNo(Integer primaryRowNo) {
        this.primaryRowNo = primaryRowNo;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
