package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMgisRectifyEntry;
import com.fc.v2.model.auto.TMgisRectifyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 承办回话。生效与否只认接口回话：回话里是落了的那一笔与单子眼下的站；
 * 同一点第二回来的，duplicated 为真，回的是头一笔——只认头一笔落笔的时辰。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "RectifyUndertakeResult", description = "承办回话")
public class RectifyUndertakeResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 落了的那一笔（同点重来时回的是头一笔） */
    @ApiModelProperty(value = "落了的那一笔")
    private final TMgisRectifyEntry entry;

    /** 单子眼下的站与轮次 */
    @ApiModelProperty(value = "单子眼下状态")
    private final TMgisRectifyOrder order;

    /** 是否同一点第二回（只认头一笔，未再落笔） */
    @ApiModelProperty(value = "是否同一点第二回")
    private final boolean duplicated;

    public RectifyUndertakeResult(TMgisRectifyEntry entry, TMgisRectifyOrder order, boolean duplicated) {
        this.entry = entry;
        this.order = order;
        this.duplicated = duplicated;
    }

    public TMgisRectifyEntry getEntry() {
        return entry;
    }

    public TMgisRectifyOrder getOrder() {
        return order;
    }

    public boolean isDuplicated() {
        return duplicated;
    }
}
