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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 测绘成果质检判定结论对象 t_mgis_qual_conclusion
 *
 * <p>处里规矩：出结论那一刻，由系统把所依据判定线的代号与启用之日写进结论，
 * 事后谁也不许手工补盖；同一幅图今天出与明天出，这一栏须一个字不差。
 * 故本表只立不改——代号、启用之日是落账当时的快照，事后改线不动旧结论；
 * 本对象不设 update_by/update_time/del_flag，服务层也不提供改、删入口。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_mgis_qual_conclusion")
@ApiModel(value = "TMgisQualConclusion", description = "测绘成果质检判定结论")
public class TMgisQualConclusion implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 图幅成果代号 */
    @TableField("item_code")
    @ApiModelProperty(value = "图幅成果代号")
    private String itemCode;

    /** 成果批号(整改零点归集按批立单；空着的不归集) */
    @TableField("batch_no")
    @ApiModelProperty(value = "成果批号")
    private String batchNo;

    /** 成果类别 TOPO_MAP地形图 IMAGE_MAP影像图 VECTOR_FEATURE矢量要素 PLACE_NAME地名注记 */
    @TableField("prod_type")
    @ApiModelProperty(value = "成果类别")
    private String prodType;

    /** 比例尺分母(如 1:2000 记 2000) */
    @TableField("scale_denominator")
    @ApiModelProperty(value = "比例尺分母")
    private Integer scaleDenominator;

    /** 交件日期(判定以交件日为准) */
    @TableField("submit_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "交件日期")
    private Date submitDate;

    /** 中误差(米) */
    @TableField("mean_error")
    @ApiModelProperty(value = "中误差(米)")
    private BigDecimal meanError;

    /** 判定线主键(备查；判定依据以快照栏为准) */
    @TableField("rule_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "判定线主键(备查)")
    private Long ruleId;

    /** 判定线代号(快照，落账即定) */
    @TableField("rule_code")
    @ApiModelProperty(value = "判定线代号(快照)")
    private String ruleCode;

    /** 判定线名称(快照) */
    @TableField("rule_name")
    @ApiModelProperty(value = "判定线名称(快照)")
    private String ruleName;

    /** 判定线启用之日(快照，落账即定) */
    @TableField("rule_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "判定线启用之日(快照)")
    private Date ruleEffStart;

    /** 档位 1一档 2二档 3三档 */
    @TableField("tier")
    @ApiModelProperty(value = "档位 1一档 2二档 3三档")
    private Integer tier;

    /** 是否恰好压在天花板上 0否 1是 */
    @TableField("on_ceiling")
    @ApiModelProperty(value = "是否压线 0否 1是")
    private Integer onCeiling;

    /** 选定缘由：凭什么是这条线 */
    @TableField("pick_reason")
    @ApiModelProperty(value = "选定缘由")
    private String pickReason;

    /** 出结论时刻 */
    @TableField("issued_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "出结论时刻")
    private Date issuedAt;

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

    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    public BigDecimal getMeanError() {
        return meanError;
    }

    public void setMeanError(BigDecimal meanError) {
        this.meanError = meanError;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Date getRuleEffStart() {
        return ruleEffStart;
    }

    public void setRuleEffStart(Date ruleEffStart) {
        this.ruleEffStart = ruleEffStart;
    }

    public Integer getTier() {
        return tier;
    }

    public void setTier(Integer tier) {
        this.tier = tier;
    }

    public Integer getOnCeiling() {
        return onCeiling;
    }

    public void setOnCeiling(Integer onCeiling) {
        this.onCeiling = onCeiling;
    }

    public String getPickReason() {
        return pickReason;
    }

    public void setPickReason(String pickReason) {
        this.pickReason = pickReason;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
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
