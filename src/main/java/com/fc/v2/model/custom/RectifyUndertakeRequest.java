package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 承办请求。门面只留「承办」一扇：告知、整改、复测、退回、销号、封存
 * 都打这一个门进，动作由 action 指认；经办与落笔时辰不收请求的话——
 * 系统在落笔那一下自己写死，事后由不得人回头补录，也不许挪时辰。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "RectifyUndertakeRequest", description = "承办请求")
public class RectifyUndertakeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 回执单主键 */
    @ApiModelProperty(value = "回执单主键", required = true)
    private Long orderId;

    /** 落笔动作 NOTIFY告知 RECTIFY整改 RETEST_AGENCY复测·检验机构 RETEST_CITY复测·属地市局 SEND_BACK退回重办 CLOSE销号 ARCHIVE封存（中文名亦可） */
    @ApiModelProperty(value = "落笔动作", required = true)
    private String action;

    /** 告知用：问题落在哪几幅图上（告知落笔即定，往后只读） */
    @ApiModelProperty(value = "问题图幅清单(仅告知站)")
    private String maps;

    /** 告知用：复测按什么口径抽样（告知落笔即定，整改站夹带即顶回） */
    @ApiModelProperty(value = "抽样口径(仅告知站)")
    private String sampling;

    /** 告知用：复测须见着的凭证（告知落笔即定） */
    @ApiModelProperty(value = "复测凭证要求(仅告知站)")
    private String evidence;

    /** 整改说明；销号、封存附言 */
    @ApiModelProperty(value = "整改说明/附言")
    private String note;

    /** 复测复核意见（检验机构、属地市局各进一笔） */
    @ApiModelProperty(value = "复测复核意见")
    private String opinion;

    /** 复测实抽幅数 */
    @ApiModelProperty(value = "复测实抽幅数")
    private Integer sampledCount;

    /** 退回缘由（退回重办必填） */
    @ApiModelProperty(value = "退回缘由")
    private String reason;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMaps() {
        return maps;
    }

    public void setMaps(String maps) {
        this.maps = maps;
    }

    public String getSampling() {
        return sampling;
    }

    public void setSampling(String sampling) {
        this.sampling = sampling;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public Integer getSampledCount() {
        return sampledCount;
    }

    public void setSampledCount(Integer sampledCount) {
        this.sampledCount = sampledCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
