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
 * 测绘成果质检判定线对象 t_mgis_qual_line
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_mgis_qual_line")
@ApiModel(value = "TMgisQualLine", description = "测绘成果质检判定线")
public class TMgisQualLine implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 成果质检判定线代号 */
    @TableField("rule_code")
    @ApiModelProperty(value = "成果质检判定线代号")
    private String ruleCode;

    /** 判定线名称 */
    @TableField("rule_name")
    @ApiModelProperty(value = "判定线名称")
    private String ruleName;

    /** 成果类别 TOPO_MAP地形图 IMAGE_MAP影像图 VECTOR_FEATURE矢量要素 PLACE_NAME地名注记 */
    @TableField("prod_type")
    @ApiModelProperty(value = "成果类别 TOPO_MAP地形图 IMAGE_MAP影像图 VECTOR_FEATURE矢量要素 PLACE_NAME地名注记")
    private String prodType;

    /** 适用比例尺档下界(分母,含)；与上界同空=未设适用档，不当通吃 */
    @TableField("scale_min")
    @ApiModelProperty(value = "适用比例尺档下界(分母,含)")
    private Integer scaleMin;

    /** 适用比例尺档上界(分母,含)；与下界同空=未设适用档，不当通吃 */
    @TableField("scale_max")
    @ApiModelProperty(value = "适用比例尺档上界(分母,含)")
    private Integer scaleMax;

    /** 一档中误差限差上限(米) */
    @TableField("th1_max")
    @ApiModelProperty(value = "一档中误差限差上限(米)")
    private BigDecimal th1Max;

    /** 二档中误差限差上限(米) */
    @TableField("th2_max")
    @ApiModelProperty(value = "二档中误差限差上限(米)")
    private BigDecimal th2Max;

    /** 三档中误差限差上限(米)；留白认作无顶，不许解成断了档 */
    @TableField("th3_max")
    @ApiModelProperty(value = "三档中误差限差上限(米)，留白认作无顶")
    private BigDecimal th3Max;

    /** 启用之日 */
    @TableField("eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "启用之日")
    private Date effStart;

    /** 交棒之日(不含)：当日起归新线；空=现行 */
    @TableField("eff_end")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "交棒之日(不含)，空=现行")
    private Date effEnd;

    /** 抢档顺位(数值越大越优先) */
    @TableField("priority")
    @ApiModelProperty(value = "抢档顺位(数值越大越优先)")
    private Integer priority;

    /** 线的情形 0当家 1已让位 */
    @TableField("status")
    @ApiModelProperty(value = "线的情形 0当家 1已让位")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

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
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getProdType() {
        return prodType;
    }

    public void setProdType(String prodType) {
        this.prodType = prodType;
    }

    public Integer getScaleMin() {
        return scaleMin;
    }

    public void setScaleMin(Integer scaleMin) {
        this.scaleMin = scaleMin;
    }

    public Integer getScaleMax() {
        return scaleMax;
    }

    public void setScaleMax(Integer scaleMax) {
        this.scaleMax = scaleMax;
    }

    public BigDecimal getTh1Max() {
        return th1Max;
    }

    public void setTh1Max(BigDecimal th1Max) {
        this.th1Max = th1Max;
    }

    public BigDecimal getTh2Max() {
        return th2Max;
    }

    public void setTh2Max(BigDecimal th2Max) {
        this.th2Max = th2Max;
    }

    public BigDecimal getTh3Max() {
        return th3Max;
    }

    public void setTh3Max(BigDecimal th3Max) {
        this.th3Max = th3Max;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }

    public Date getEffEnd() {
        return effEnd;
    }

    public void setEffEnd(Date effEnd) {
        this.effEnd = effEnd;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
