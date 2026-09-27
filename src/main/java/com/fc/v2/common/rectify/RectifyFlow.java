package com.fc.v2.common.rectify;

import com.alibaba.fastjson.JSONObject;
import com.fc.v2.model.auto.TMgisRectifyEntry;
import com.fc.v2.model.auto.TMgisRectifyOrder;
import com.fc.v2.model.custom.RectifyUndertakeRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 整改走站引擎：回执单规矩在系统里的唯一出处。
 *
 * <p>一批成果从挂出问题到销号，全程只凭系统里这一张整改回执单，一单对一批，
 * 单上分六站：挂号、告知、整改、复测、销号、封存。走站只许一站接一站，
 * 章程全收在这一个类里，服务层、控制器都从这里过，别处不许另写一套。</p>
 *
 * <ul>
 *   <li><b>告知三样落笔即定</b>：问题落在哪几幅图、复测按什么口径抽样、复测须见着的凭证，
 *       自告知落笔起固定住，往后的站只读取不重填；整改那一站想改抽样口径，当场顶回。</li>
 *   <li><b>复测认两个人</b>：检验机构一名、属地市局一名，两名的复核意见都要进单，
 *       只回来一名这一站不算走通。</li>
 *   <li><b>只许一站接一站</b>：单子停在哪一站就只开哪一站的口子，隔着站落笔不生效。</li>
 *   <li><b>退回一站</b>：复测上看出抽样幅数与告知合不上，把整改退回重办，轮次加一；
 *       没有别的回头路。</li>
 *   <li><b>封存即定案</b>：封了存的单子不再挂新条目，要重查另行立单，两张各算各的。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-09-26
 */
public final class RectifyFlow {

    private RectifyFlow() {
    }

    /** 检出口径：判定结论落到三档即不合格件，零点归集立单只认这一档 */
    public static final int NONCONFORMING_TIER = 3;

    /** 在途 */
    public static final int STATUS_OPEN = 0;
    /** 封存 */
    public static final int STATUS_SEALED = 1;

    // ---- 落笔内容键（告知三样自落笔起固定，往后各站只读取不重填） ----
    /** 问题落在哪几幅图上 */
    public static final String KEY_MAPS = "maps";
    /** 复测按什么口径抽样 */
    public static final String KEY_SAMPLING = "sampling";
    /** 复测须见着的凭证 */
    public static final String KEY_EVIDENCE = "evidence";
    /** 整改说明 / 销号、封存附言 */
    public static final String KEY_NOTE = "note";
    /** 复测复核意见 */
    public static final String KEY_OPINION = "opinion";
    /** 复测实抽幅数 */
    public static final String KEY_SAMPLED_COUNT = "sampledCount";
    /** 退回缘由 */
    public static final String KEY_REASON = "reason";

    /**
     * 承办前核站：单子停在哪一站就只开哪一站的口子。
     * 封存后的单子什么动作都不收；挂号是零点归集的笔，承办门不收。
     */
    public static void checkUndertake(TMgisRectifyOrder order, RectifyAction action) {
        checkWritable(order, action);
        checkStation(order, action);
    }

    /**
     * 核单子还收不收笔：封存后的单子什么动作都不收（要重查另行立单）；
     * 挂号是零点归集的笔，承办门不收。
     */
    public static void checkWritable(TMgisRectifyOrder order, RectifyAction action) {
        if (order == null) {
            throw new RectifyFlowException("单子不在账上");
        }
        if (action == null) {
            throw new IllegalArgumentException("落笔动作未指");
        }
        if (order.getStatus() != null && order.getStatus() == STATUS_SEALED) {
            throw new RectifyFlowException("单子已封存：封了存的单子不再挂新条目，也不许把哪一站整条摘除；"
                    + "要重查请另行立单，从挂号重新走满六站");
        }
        if (action == RectifyAction.REGISTER) {
            throw new RectifyFlowException("挂号由零点归集落笔，承办门不收这一笔");
        }
    }

    /**
     * 核这一笔够不够得着：单子眼下停在哪一站，就只开哪一站的口子，
     * 隔着站落笔不生效。
     */
    public static void checkStation(TMgisRectifyOrder order, RectifyAction action) {
        RectifyStation current = RectifyStation.require(order.getStation());
        if (action.getStation() != current) {
            throw new RectifyFlowException("单子眼下停在「" + current.getLabel() + "」这一站，只开这一站的口子；「"
                    + action.getLabel() + "」这一笔隔着站，不生效");
        }
    }

    /**
     * 把承办请求归置成这一笔要写的话（JSON）。
     * 各站该说什么、不许说什么都定死在这里：
     * 告知三样缺一样落不了笔；整改站夹带告知三样，当场顶回——
     * 改出来的说法也不许把先前落笔那一笔盖掉。
     */
    public static String packPayload(RectifyAction action, RectifyUndertakeRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("承办请求为空");
        }
        JSONObject payload = new JSONObject(true);
        switch (action) {
            case NOTIFY:
                payload.put(KEY_MAPS, requireText(req.getMaps(), "告知三样须说定：问题落在哪几幅图上未填"));
                payload.put(KEY_SAMPLING, requireText(req.getSampling(), "告知三样须说定：复测按什么口径抽样未填"));
                payload.put(KEY_EVIDENCE, requireText(req.getEvidence(), "告知三样须说定：复测须见着的凭证未填"));
                break;
            case RECTIFY:
                if (hasText(req.getMaps()) || hasText(req.getSampling()) || hasText(req.getEvidence())) {
                    throw new RectifyFlowException("告知落笔的三样只读取不重填：整改这一站想改抽样口径，当场顶回");
                }
                payload.put(KEY_NOTE, requireText(req.getNote(), "整改说明未填"));
                break;
            case RETEST_AGENCY:
            case RETEST_CITY:
                payload.put(KEY_OPINION, requireText(req.getOpinion(),
                        "复核意见未填：检验机构、属地市局两名的意见都要进单"));
                if (req.getSampledCount() != null) {
                    payload.put(KEY_SAMPLED_COUNT, req.getSampledCount());
                }
                break;
            case SEND_BACK:
                payload.put(KEY_REASON, requireText(req.getReason(), "退回缘由未填"));
                break;
            case CLOSE:
            case ARCHIVE:
                if (hasText(req.getNote())) {
                    payload.put(KEY_NOTE, req.getNote().trim());
                }
                break;
            default:
                throw new RectifyFlowException("挂号由零点归集落笔，承办门不收这一笔");
        }
        return payload.toJSONString();
    }

    /**
     * 落笔之后单子挪到哪一站。复测那一站认两个人：两名的复核意见都进了单，
     * 这一站才算走通；只回来一名，单子仍停在复测。
     *
     * @param action       刚落的这一笔
     * @param retestBothIn 复测两名意见是否都已进单（仅复测动作用得上）
     */
    public static RectifyStation stationAfter(RectifyAction action, boolean retestBothIn) {
        switch (action) {
            case REGISTER:
                return RectifyStation.NOTIFY;
            case NOTIFY:
                return RectifyStation.RECTIFY;
            case RECTIFY:
                return RectifyStation.RETEST;
            case RETEST_AGENCY:
            case RETEST_CITY:
                return retestBothIn ? RectifyStation.CLOSE : RectifyStation.RETEST;
            case SEND_BACK:
                // 退回一站：把整改退回重办，轮次加一（由服务层记）
                return RectifyStation.RECTIFY;
            case CLOSE:
                return RectifyStation.ARCHIVE;
            case ARCHIVE:
            default:
                return RectifyStation.ARCHIVE;
        }
    }

    /** 这一笔是不是退回重办（是则轮次加一） */
    public static boolean isSendBack(RectifyAction action) {
        return action == RectifyAction.SEND_BACK;
    }

    /**
     * 链上前后两笔的走站次序合不合法：
     * 同轮只许原地补复测的另一名，或往下一站挪；
     * 跨轮只许「退回重办 → 整改重办」这一种。
     */
    public static boolean legalTransition(TMgisRectifyEntry prev, TMgisRectifyEntry next) {
        if (prev == null || next == null) {
            return false;
        }
        RectifyAction pa = RectifyAction.require(prev.getAction());
        RectifyAction na = RectifyAction.require(next.getAction());
        if (!pa.getStation().name().equals(prev.getStation()) || !na.getStation().name().equals(next.getStation())) {
            return false;
        }
        int dr = roundOf(next) - roundOf(prev);
        if (dr == 0) {
            if (pa.getStation() == na.getStation()) {
                if (pa.getStation() != RectifyStation.RETEST) {
                    return false;
                }
                // 复测站上同轮：两名先后进单，或一名进单后看出合不上退回
                return (pa == RectifyAction.RETEST_AGENCY || pa == RectifyAction.RETEST_CITY)
                        && (na == RectifyAction.RETEST_AGENCY || na == RectifyAction.RETEST_CITY
                        || na == RectifyAction.SEND_BACK)
                        && pa != na;
            }
            return na.getStation().getOrder() == pa.getStation().getOrder() + 1;
        }
        if (dr == 1) {
            return pa == RectifyAction.SEND_BACK && na == RectifyAction.RECTIFY;
        }
        return false;
    }

    /**
     * 页面口径：每一站只显最新一轮落笔的话。
     * 退回重办后，那一站原先写的话退出页面，只显重办后的那一份——
     * 两代话并排挂着，来检查的人就要问哪一份算数；链上两笔都还在，页面上只留算数的那笔。
     *
     * @return 站代号 → 该站算数的落笔（按落笔先后排）
     */
    public static Map<String, List<TMgisRectifyEntry>> pageOf(List<TMgisRectifyEntry> entries) {
        Map<String, List<TMgisRectifyEntry>> page = new LinkedHashMap<String, List<TMgisRectifyEntry>>();
        for (RectifyStation station : RectifyStation.values()) {
            int maxRound = Integer.MIN_VALUE;
            if (entries != null) {
                for (TMgisRectifyEntry e : entries) {
                    if (station.name().equals(e.getStation()) && roundOf(e) > maxRound) {
                        maxRound = roundOf(e);
                    }
                }
                if (maxRound == Integer.MIN_VALUE) {
                    continue;
                }
                List<TMgisRectifyEntry> shown = new ArrayList<TMgisRectifyEntry>();
                for (TMgisRectifyEntry e : entries) {
                    if (station.name().equals(e.getStation()) && roundOf(e) == maxRound) {
                        shown.add(e);
                    }
                }
                page.put(station.name(), shown);
            }
        }
        return page;
    }

    /** 站代号译站名，译不得照原样回 */
    public static String labelOf(String stationName) {
        try {
            return RectifyStation.require(stationName).getLabel();
        } catch (IllegalArgumentException e) {
            return String.valueOf(stationName);
        }
    }

    private static int roundOf(TMgisRectifyEntry e) {
        return e.getRoundNo() == null ? 1 : e.getRoundNo();
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String requireText(String s, String message) {
        if (!hasText(s)) {
            throw new RectifyFlowException(message);
        }
        return s.trim();
    }
}
