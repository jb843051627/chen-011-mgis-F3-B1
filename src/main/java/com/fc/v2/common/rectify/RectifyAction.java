package com.fc.v2.common.rectify;

/**
 * 落笔动作。每个动作归哪一站写死在这里：
 * 复测站上三个动作——检验机构复核、属地市局复核、退回重办；
 * 退回只许从复测退回整改（退回一站），没有别的回头路。
 *
 * @author fuce
 * @date 2026-09-26
 */
public enum RectifyAction {

    /** 挂号（零点归集立单时系统落笔，承办门不收） */
    REGISTER("挂号", RectifyStation.REGISTER),
    /** 告知：说定三样，落笔即定 */
    NOTIFY("告知", RectifyStation.NOTIFY),
    /** 整改 */
    RECTIFY("整改", RectifyStation.RECTIFY),
    /** 复测复核意见·检验机构 */
    RETEST_AGENCY("复测复核·检验机构", RectifyStation.RETEST),
    /** 复测复核意见·属地市局 */
    RETEST_CITY("复测复核·属地市局", RectifyStation.RETEST),
    /** 退回重办：复测上看出抽样幅数与告知合不上，把整改退回 */
    SEND_BACK("退回重办", RectifyStation.RETEST),
    /** 销号 */
    CLOSE("销号", RectifyStation.CLOSE),
    /** 封存 */
    ARCHIVE("封存", RectifyStation.ARCHIVE);

    private final String label;
    private final RectifyStation station;

    RectifyAction(String label, RectifyStation station) {
        this.label = label;
        this.station = station;
    }

    public String getLabel() {
        return label;
    }

    /** 这一笔落在哪一站 */
    public RectifyStation getStation() {
        return station;
    }

    /**
     * 按代号或中文名认领动作；认不得即驳回。
     */
    public static RectifyAction require(String code) {
        if (code != null) {
            String s = code.trim();
            for (RectifyAction a : values()) {
                if (a.name().equalsIgnoreCase(s) || a.label.equals(s)) {
                    return a;
                }
            }
        }
        throw new IllegalArgumentException("落笔动作不识：" + code);
    }
}
