package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMgisRectifyEntry;
import com.fc.v2.model.auto.TMgisRectifyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 回执单页面：单子眼下停在哪一站，就只开哪一站的口子；
 * 每一站只显最新一轮落笔的话——退回重办后，原先写的话退出页面，
 * 只显重办后的那一份，链上两笔都还在（查链走 /mgisRectify/{id}/chain）。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "RectifyPageView", description = "回执单页面")
public class RectifyPageView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 单子 */
    @ApiModelProperty(value = "单子")
    private final TMgisRectifyOrder order;

    /** 眼下停在哪一站 */
    @ApiModelProperty(value = "眼下停在哪一站")
    private final String currentStation;

    /** 是否已封存 */
    @ApiModelProperty(value = "是否已封存")
    private final boolean sealed;

    /** 逐站页面（六站全列，未到的站空着） */
    @ApiModelProperty(value = "逐站页面")
    private final List<StationPage> stations;

    public RectifyPageView(TMgisRectifyOrder order, String currentStation, boolean sealed,
                           List<StationPage> stations) {
        this.order = order;
        this.currentStation = currentStation;
        this.sealed = sealed;
        this.stations = stations;
    }

    public TMgisRectifyOrder getOrder() {
        return order;
    }

    public String getCurrentStation() {
        return currentStation;
    }

    public boolean isSealed() {
        return sealed;
    }

    public List<StationPage> getStations() {
        return stations;
    }

    /**
     * 一站的页面：开不开口子、站上算数的几笔。
     */
    @ApiModel(value = "RectifyStationPage", description = "一站的页面")
    public static class StationPage implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 站代号 */
        @ApiModelProperty(value = "站代号")
        private final String station;

        /** 站名 */
        @ApiModelProperty(value = "站名")
        private final String label;

        /** 这一站眼下开不开口子（只开眼下停的那一站） */
        @ApiModelProperty(value = "这一站开不开口子")
        private final boolean open;

        /** 这一站算数的落笔（最新一轮；被顶替的已退出页面） */
        @ApiModelProperty(value = "这一站算数的落笔")
        private final List<TMgisRectifyEntry> entries;

        public StationPage(String station, String label, boolean open, List<TMgisRectifyEntry> entries) {
            this.station = station;
            this.label = label;
            this.open = open;
            this.entries = entries;
        }

        public String getStation() {
            return station;
        }

        public String getLabel() {
            return label;
        }

        public boolean isOpen() {
            return open;
        }

        public List<TMgisRectifyEntry> getEntries() {
            return entries;
        }
    }
}
