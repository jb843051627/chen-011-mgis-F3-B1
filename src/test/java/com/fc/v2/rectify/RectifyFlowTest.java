package com.fc.v2.rectify;

import com.fc.v2.common.rectify.RectifyAction;
import com.fc.v2.common.rectify.RectifyFlow;
import com.fc.v2.common.rectify.RectifyFlowException;
import com.fc.v2.common.rectify.RectifyStation;
import com.fc.v2.model.auto.TMgisRectifyEntry;
import com.fc.v2.model.auto.TMgisRectifyOrder;
import com.fc.v2.model.custom.RectifyUndertakeRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 走站章程单测：回执单规矩逐条对号。
 */
class RectifyFlowTest {

    private static TMgisRectifyOrder orderAt(RectifyStation station, int round, int status) {
        TMgisRectifyOrder o = new TMgisRectifyOrder();
        o.setId(1L);
        o.setStation(station.name());
        o.setRoundNo(round);
        o.setStatus(status);
        return o;
    }

    private static RectifyUndertakeRequest req(RectifyAction action) {
        RectifyUndertakeRequest r = new RectifyUndertakeRequest();
        r.setOrderId(1L);
        r.setAction(action.name());
        return r;
    }

    private static TMgisRectifyEntry pen(RectifyAction action, int round) {
        TMgisRectifyEntry e = new TMgisRectifyEntry();
        e.setStation(action.getStation().name());
        e.setAction(action.name());
        e.setRoundNo(round);
        return e;
    }

    // ---------- 告知三样 ----------

    @Test
    void 告知三样缺一样落不了笔() {
        RectifyUndertakeRequest r = req(RectifyAction.NOTIFY);
        r.setMaps("图幅A、图幅B");
        r.setSampling("按批抽百分之十");
        // 凭证未说定
        RectifyFlowException e = assertThrows(RectifyFlowException.class,
                () -> RectifyFlow.packPayload(RectifyAction.NOTIFY, r));
        assertTrue(e.getMessage().contains("凭证"));
    }

    @Test
    void 告知三样说定才落笔() {
        RectifyUndertakeRequest r = req(RectifyAction.NOTIFY);
        r.setMaps("图幅A、图幅B");
        r.setSampling("按批抽百分之十");
        r.setEvidence("复测报告原件");
        String payload = RectifyFlow.packPayload(RectifyAction.NOTIFY, r);
        assertTrue(payload.contains("图幅A、图幅B"));
        assertTrue(payload.contains("按批抽百分之十"));
        assertTrue(payload.contains("复测报告原件"));
    }

    // ---------- 整改站不许动告知三样 ----------

    @Test
    void 整改站想改抽样口径当场顶回() {
        RectifyUndertakeRequest r = req(RectifyAction.RECTIFY);
        r.setNote("已重测");
        r.setSampling("改成全数检查");
        RectifyFlowException e = assertThrows(RectifyFlowException.class,
                () -> RectifyFlow.packPayload(RectifyAction.RECTIFY, r));
        assertTrue(e.getMessage().contains("当场顶回"));
    }

    @Test
    void 整改站夹带问题图幅或凭证一样顶回() {
        RectifyUndertakeRequest r1 = req(RectifyAction.RECTIFY);
        r1.setNote("已重测");
        r1.setMaps("换成图幅C");
        assertThrows(RectifyFlowException.class, () -> RectifyFlow.packPayload(RectifyAction.RECTIFY, r1));
        RectifyUndertakeRequest r2 = req(RectifyAction.RECTIFY);
        r2.setNote("已重测");
        r2.setEvidence("凭证免了");
        assertThrows(RectifyFlowException.class, () -> RectifyFlow.packPayload(RectifyAction.RECTIFY, r2));
    }

    @Test
    void 整改说明正常落笔() {
        RectifyUndertakeRequest r = req(RectifyAction.RECTIFY);
        r.setNote("两幅图已按告知重测整改");
        String payload = RectifyFlow.packPayload(RectifyAction.RECTIFY, r);
        assertTrue(payload.contains("两幅图已按告知重测整改"));
        assertFalse(payload.contains(RectifyFlow.KEY_SAMPLING));
    }

    // ---------- 复测认两个人 ----------

    @Test
    void 复测缺意见落不了笔() {
        RectifyUndertakeRequest r = req(RectifyAction.RETEST_AGENCY);
        RectifyFlowException e = assertThrows(RectifyFlowException.class,
                () -> RectifyFlow.packPayload(RectifyAction.RETEST_AGENCY, r));
        assertTrue(e.getMessage().contains("两名"));
    }

    @Test
    void 复测只回来一名不算走通() {
        // 只回来检验机构一名：单子仍停在复测
        assertEquals(RectifyStation.RETEST, RectifyFlow.stationAfter(RectifyAction.RETEST_AGENCY, false));
        assertEquals(RectifyStation.RETEST, RectifyFlow.stationAfter(RectifyAction.RETEST_CITY, false));
        // 两名都进了单，这一站才算走通
        assertEquals(RectifyStation.CLOSE, RectifyFlow.stationAfter(RectifyAction.RETEST_AGENCY, true));
        assertEquals(RectifyStation.CLOSE, RectifyFlow.stationAfter(RectifyAction.RETEST_CITY, true));
    }

    // ---------- 只许一站接一站 ----------

    @Test
    void 隔着站落笔不生效() {
        TMgisRectifyOrder order = orderAt(RectifyStation.NOTIFY, 1, RectifyFlow.STATUS_OPEN);
        RectifyFlowException e = assertThrows(RectifyFlowException.class,
                () -> RectifyFlow.checkUndertake(order, RectifyAction.RECTIFY));
        assertTrue(e.getMessage().contains("隔着站"));
        // 当前站的动作收
        RectifyFlow.checkUndertake(order, RectifyAction.NOTIFY);
    }

    @Test
    void 封存后什么动作都顶回() {
        TMgisRectifyOrder sealed = orderAt(RectifyStation.ARCHIVE, 1, RectifyFlow.STATUS_SEALED);
        RectifyFlowException e = assertThrows(RectifyFlowException.class,
                () -> RectifyFlow.checkUndertake(sealed, RectifyAction.ARCHIVE));
        assertTrue(e.getMessage().contains("封存"));
    }

    @Test
    void 挂号笔承办门不收() {
        TMgisRectifyOrder order = orderAt(RectifyStation.NOTIFY, 1, RectifyFlow.STATUS_OPEN);
        assertThrows(RectifyFlowException.class,
                () -> RectifyFlow.checkUndertake(order, RectifyAction.REGISTER));
    }

    @Test
    void 退回只许退回复测前那一站() {
        assertEquals(RectifyStation.RECTIFY, RectifyFlow.stationAfter(RectifyAction.SEND_BACK, false));
        assertTrue(RectifyFlow.isSendBack(RectifyAction.SEND_BACK));
        assertFalse(RectifyFlow.isSendBack(RectifyAction.RECTIFY));
    }

    @Test
    void 退回缘由未填顶回() {
        RectifyUndertakeRequest r = req(RectifyAction.SEND_BACK);
        assertThrows(RectifyFlowException.class, () -> RectifyFlow.packPayload(RectifyAction.SEND_BACK, r));
    }

    // ---------- 链上走站次序 ----------

    @Test
    void 顺走六站次序都合法() {
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.REGISTER, 1), pen(RectifyAction.NOTIFY, 1)));
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.NOTIFY, 1), pen(RectifyAction.RECTIFY, 1)));
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.RECTIFY, 1), pen(RectifyAction.RETEST_AGENCY, 1)));
        // 复测两名同站先后进单
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.RETEST_AGENCY, 1), pen(RectifyAction.RETEST_CITY, 1)));
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.RETEST_CITY, 1), pen(RectifyAction.RETEST_AGENCY, 1)));
        // 一名进单后看出合不上，退回重办
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.RETEST_AGENCY, 1), pen(RectifyAction.SEND_BACK, 1)));
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.RETEST_CITY, 1), pen(RectifyAction.SEND_BACK, 1)));
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.RETEST_CITY, 1), pen(RectifyAction.CLOSE, 1)));
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.CLOSE, 1), pen(RectifyAction.ARCHIVE, 1)));
    }

    @Test
    void 跳站同站重名都错乱() {
        // 告知直接跳到复测：隔站
        assertFalse(RectifyFlow.legalTransition(pen(RectifyAction.NOTIFY, 1), pen(RectifyAction.RETEST_AGENCY, 1)));
        // 复测同一名进两笔
        assertFalse(RectifyFlow.legalTransition(pen(RectifyAction.RETEST_AGENCY, 1), pen(RectifyAction.RETEST_AGENCY, 1)));
        // 整改同站再来一笔（同轮）
        assertFalse(RectifyFlow.legalTransition(pen(RectifyAction.RECTIFY, 1), pen(RectifyAction.RECTIFY, 1)));
        // 倒着走（不经退回）
        assertFalse(RectifyFlow.legalTransition(pen(RectifyAction.RETEST_CITY, 1), pen(RectifyAction.RECTIFY, 1)));
        // 退回之后不挪轮次，或退回接的不是整改，都不认
        assertFalse(RectifyFlow.legalTransition(pen(RectifyAction.SEND_BACK, 1), pen(RectifyAction.RECTIFY, 1)));
        assertFalse(RectifyFlow.legalTransition(pen(RectifyAction.SEND_BACK, 1), pen(RectifyAction.SEND_BACK, 1)));
    }

    @Test
    void 跨轮只许退回接整改() {
        assertTrue(RectifyFlow.legalTransition(pen(RectifyAction.SEND_BACK, 1), pen(RectifyAction.RECTIFY, 2)));
        // 不是退回撑起来的跨轮，不认
        assertFalse(RectifyFlow.legalTransition(pen(RectifyAction.RETEST_CITY, 1), pen(RectifyAction.RECTIFY, 2)));
        assertFalse(RectifyFlow.legalTransition(pen(RectifyAction.SEND_BACK, 1), pen(RectifyAction.RETEST_AGENCY, 2)));
    }

    // ---------- 页面口径：重办后旧话退出页面 ----------

    @Test
    void 重办后那一站只显重办后的那一份() {
        TMgisRectifyEntry notify = pen(RectifyAction.NOTIFY, 1);
        TMgisRectifyEntry rectifyOld = pen(RectifyAction.RECTIFY, 1);
        TMgisRectifyEntry sendBack = pen(RectifyAction.SEND_BACK, 1);
        TMgisRectifyEntry rectifyNew = pen(RectifyAction.RECTIFY, 2);
        List<TMgisRectifyEntry> chain = Arrays.asList(notify, rectifyOld, sendBack, rectifyNew);
        Map<String, List<TMgisRectifyEntry>> page = RectifyFlow.pageOf(chain);
        // 整改站只显第 2 轮那一份，原先写的退出页面
        assertEquals(1, page.get(RectifyStation.RECTIFY.name()).size());
        assertTrue(page.get(RectifyStation.RECTIFY.name()).get(0) == rectifyNew);
        // 告知落笔即定，不受重办影响
        assertEquals(1, page.get(RectifyStation.NOTIFY.name()).size());
        // 复测站还停在第 1 轮：退回那一笔在页面上
        assertEquals(1, page.get(RectifyStation.RETEST.name()).size());
        assertTrue(page.get(RectifyStation.RETEST.name()).get(0) == sendBack);
    }
}
