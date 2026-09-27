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
 * 整改回执单对象 t_mgis_rectify_order
 *
 * <p>一单对一批：同一批成果只要还有一张单子在途，第二张便立不出来
 * （批号 + 在途标记联合唯一）。单子只由零点归集立起，承办门不起单；
 * 告知三样自告知落笔起快照进单，往后的站只读取不重填。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_mgis_rectify_order")
@ApiModel(value = "TMgisRectifyOrder", description = "整改回执单")
public class TMgisRectifyOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 单号 */
    @TableField("order_no")
    @ApiModelProperty(value = "单号")
    private String orderNo;

    /** 成果批号(一单对一批) */
    @TableField("batch_no")
    @ApiModelProperty(value = "成果批号")
    private String batchNo;

    /** 眼下停在哪一站 NOTIFY告知 RECTIFY整改 RETEST复测 CLOSE销号 ARCHIVE封存 */
    @TableField("station")
    @ApiModelProperty(value = "眼下停在哪一站")
    private String station;

    /** 第几轮(退回重办一轮加一) */
    @TableField("round_no")
    @ApiModelProperty(value = "第几轮")
    private Integer roundNo;

    /** 状态 0在途 1封存 */
    @TableField("status")
    @ApiModelProperty(value = "状态 0在途 1封存")
    private Integer status;

    /** 在途标记 1在途 NULL封存；与批号联合唯一——在途第二张立不出来 */
    @TableField("in_flight")
    @ApiModelProperty(value = "在途标记 1在途 NULL封存")
    private Integer inFlight;

    /** 归集的检出日(前一日) */
    @TableField("detect_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "归集的检出日")
    private Date detectDate;

    /** 归集件数 */
    @TableField("item_count")
    @ApiModelProperty(value = "归集件数")
    private Integer itemCount;

    /** 告知快照:问题落在哪几幅图(落笔即定,往后只读) */
    @TableField("notify_maps")
    @ApiModelProperty(value = "告知快照:问题图幅")
    private String notifyMaps;

    /** 告知快照:复测按什么口径抽样(落笔即定,往后只读) */
    @TableField("notify_sampling")
    @ApiModelProperty(value = "告知快照:抽样口径")
    private String notifySampling;

    /** 告知快照:复测须见着的凭证(落笔即定,往后只读) */
    @TableField("notify_evidence")
    @ApiModelProperty(value = "告知快照:复测凭证")
    private String notifyEvidence;

    /** 告知经办(快照) */
    @TableField("notify_by")
    @ApiModelProperty(value = "告知经办(快照)")
    private String notifyBy;

    /** 告知落笔时辰(快照) */
    @TableField("notify_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "告知落笔时辰(快照)")
    private Date notifyAt;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getInFlight() {
        return inFlight;
    }

    public void setInFlight(Integer inFlight) {
        this.inFlight = inFlight;
    }

    public Date getDetectDate() {
        return detectDate;
    }

    public void setDetectDate(Date detectDate) {
        this.detectDate = detectDate;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public String getNotifyMaps() {
        return notifyMaps;
    }

    public void setNotifyMaps(String notifyMaps) {
        this.notifyMaps = notifyMaps;
    }

    public String getNotifySampling() {
        return notifySampling;
    }

    public void setNotifySampling(String notifySampling) {
        this.notifySampling = notifySampling;
    }

    public String getNotifyEvidence() {
        return notifyEvidence;
    }

    public void setNotifyEvidence(String notifyEvidence) {
        this.notifyEvidence = notifyEvidence;
    }

    public String getNotifyBy() {
        return notifyBy;
    }

    public void setNotifyBy(String notifyBy) {
        this.notifyBy = notifyBy;
    }

    public Date getNotifyAt() {
        return notifyAt;
    }

    public void setNotifyAt(Date notifyAt) {
        this.notifyAt = notifyAt;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
