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
 * 年度图幅目录册对象 t_mgis_catalog_book。
 *
 * <p>一册递进来立一条，册号唯一：同册号送来第二遍，只认最早那一遍，
 * 后到的顶不掉先收的。只立不改：不设 update/del_flag 列，
 * 服务层无单条添改、删除入口；确实要改数，走差异清单，不在核收记录里追加。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_mgis_catalog_book")
@ApiModel(value = "TMgisCatalogBook", description = "年度图幅目录册")
public class TMgisCatalogBook implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 册号(一册一号,唯一;重递只认头一遍) */
    @TableField("book_no")
    @ApiModelProperty(value = "册号")
    private String bookNo;

    /** 递交属地(十一市册子抬头) */
    @TableField("submit_region")
    @ApiModelProperty(value = "递交属地")
    private String submitRegion;

    /** 表上总行数(系统按原表逐行数得) */
    @TableField("total_rows")
    @ApiModelProperty(value = "表上总行数")
    private Integer totalRows;

    /** 进账行数(挂账+进账=总行数;被并掉的重复行仍算进账行) */
    @TableField("accepted_rows")
    @ApiModelProperty(value = "进账行数")
    private Integer acceptedRows;

    /** 挂账行数(一处填错只挂这一行,不连坐) */
    @TableField("pending_rows")
    @ApiModelProperty(value = "挂账行数")
    private Integer pendingRows;

    /** 进账条目数(同图幅代号并成一条后的条数) */
    @TableField("entry_count")
    @ApiModelProperty(value = "进账条目数")
    private Integer entryCount;

    /** 进账幅数合计(同代号只按头一行加一次) */
    @TableField("sheet_total")
    @ApiModelProperty(value = "进账幅数合计")
    private Integer sheetTotal;

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

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getAcceptedRows() {
        return acceptedRows;
    }

    public void setAcceptedRows(Integer acceptedRows) {
        this.acceptedRows = acceptedRows;
    }

    public Integer getPendingRows() {
        return pendingRows;
    }

    public void setPendingRows(Integer pendingRows) {
        this.pendingRows = pendingRows;
    }

    public Integer getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(Integer entryCount) {
        this.entryCount = entryCount;
    }

    public Integer getSheetTotal() {
        return sheetTotal;
    }

    public void setSheetTotal(Integer sheetTotal) {
        this.sheetTotal = sheetTotal;
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
