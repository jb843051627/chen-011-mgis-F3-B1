package com.fc.v2.rectify;

import com.fc.v2.common.rectify.RectifyAction;
import com.fc.v2.common.rectify.RectifyChainJudge;
import com.fc.v2.common.rectify.RectifyStation;
import com.fc.v2.model.auto.TMgisRectifyEntry;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 落笔链单测：顺拉倒拉，少一笔、错一处、事后补录都算不合格。
 */
class RectifyChainJudgeTest {

    private static final long ORDER_ID = 7L;

    private static Date at(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** 往链尾落一笔（哈希按章程押好） */
    private static TMgisRectifyEntry pen(List<TMgisRectifyEntry> chain, RectifyAction action,
                                         int round, String payload, String by, String at) {
        TMgisRectifyEntry e = new TMgisRectifyEntry();
        e.setOrderId(ORDER_ID);
        e.setSeq(chain.size() + 1);
        e.setRoundNo(round);
        e.setStation(action.getStation().name());
        e.setAction(action.name());
        e.setPayload(payload);
        e.setPennedBy(by);
        e.setPennedAt(at(at));
        e.setPrevHash(chain.isEmpty() ? RectifyChainJudge.GENESIS
                : chain.get(chain.size() - 1).getHash());
        e.setHash(RectifyChainJudge.hashOf(e));
        chain.add(e);
        return e;
    }

    /** 一张走满六站的单子（含一次退回重办） */
    private static List<TMgisRectifyEntry> fullChain() {
        List<TMgisRectifyEntry> chain = new ArrayList<TMgisRectifyEntry>();
        pen(chain, RectifyAction.REGISTER, 1, "{\"source\":\"零点归集\"}", "系统归集", "2026-09-20 00:00:01");
        pen(chain, RectifyAction.NOTIFY, 1, "{\"maps\":\"图幅A\"}", "张三", "2026-09-20 09:00:00");
        pen(chain, RectifyAction.RECTIFY, 1, "{\"note\":\"已整改\"}", "李四", "2026-09-21 10:00:00");
        pen(chain, RectifyAction.RETEST_AGENCY, 1, "{\"opinion\":\"合格\"}", "王五", "2026-09-22 11:00:00");
        pen(chain, RectifyAction.SEND_BACK, 1, "{\"reason\":\"抽样幅数合不上\"}", "赵六", "2026-09-22 15:00:00");
        pen(chain, RectifyAction.RECTIFY, 2, "{\"note\":\"重新整改\"}", "李四", "2026-09-23 09:00:00");
        pen(chain, RectifyAction.RETEST_AGENCY, 2, "{\"opinion\":\"合格\"}", "王五", "2026-09-24 10:00:00");
        pen(chain, RectifyAction.RETEST_CITY, 2, "{\"opinion\":\"同意\"}", "孙七", "2026-09-24 11:00:00");
        pen(chain, RectifyAction.CLOSE, 2, "{}", "张三", "2026-09-25 09:00:00");
        pen(chain, RectifyAction.ARCHIVE, 2, "{}", "张三", "2026-09-25 17:00:00");
        return chain;
    }

    @Test
    void 顺拉倒拉皆通才算合格() {
        List<TMgisRectifyEntry> chain = fullChain();
        assertTrue(RectifyChainJudge.verifyForward(chain).isOk());
        assertTrue(RectifyChainJudge.verifyBackward(chain).isOk());
    }

    @Test
    void 少一笔即不合格() {
        List<TMgisRectifyEntry> chain = fullChain();
        chain.remove(3); // 抽走复测那一笔
        RectifyChainJudge.Leg forward = RectifyChainJudge.verifyForward(chain);
        assertFalse(forward.isOk());
        // 顺拉在断点处接不住前一笔
        assertFalse(RectifyChainJudge.verifyBackward(chain).isOk());
    }

    @Test
    void 序号错位即不合格() {
        List<TMgisRectifyEntry> chain = fullChain();
        // 两笔换个位置
        TMgisRectifyEntry a = chain.get(2);
        chain.set(2, chain.get(3));
        chain.set(3, a);
        assertFalse(RectifyChainJudge.verifyForward(chain).isOk());
    }

    @Test
    void 跳站即次序错乱() {
        List<TMgisRectifyEntry> chain = new ArrayList<TMgisRectifyEntry>();
        pen(chain, RectifyAction.REGISTER, 1, "{}", "系统归集", "2026-09-20 00:00:01");
        pen(chain, RectifyAction.NOTIFY, 1, "{}", "张三", "2026-09-20 09:00:00");
        // 告知完直接复测，整改那一站跳过去
        pen(chain, RectifyAction.RETEST_AGENCY, 1, "{}", "王五", "2026-09-21 10:00:00");
        RectifyChainJudge.Leg forward = RectifyChainJudge.verifyForward(chain);
        assertFalse(forward.isOk());
        assertTrue(forward.getMessage().contains("次序"));
    }

    @Test
    void 事后改话即不合格() {
        List<TMgisRectifyEntry> chain = fullChain();
        // 把整改那一笔的话改了，哈希还是旧的
        chain.get(2).setPayload("{\"note\":\"改过的说法\"}");
        RectifyChainJudge.Leg forward = RectifyChainJudge.verifyForward(chain);
        assertFalse(forward.isOk());
        assertTrue(forward.getMessage().contains("哈希"));
    }

    @Test
    void 事后换经办一样不合格() {
        List<TMgisRectifyEntry> chain = fullChain();
        chain.get(2).setPennedBy("顶替的人");
        assertFalse(RectifyChainJudge.verifyForward(chain).isOk());
    }

    @Test
    void 挪时辰即不合格() {
        List<TMgisRectifyEntry> chain = fullChain();
        // 把第 4 笔的时辰挪到头里去，并重押哈希（单改时辰不改哈希会先撞哈希关）
        TMgisRectifyEntry e = chain.get(3);
        e.setPennedAt(at("2026-09-19 08:00:00"));
        e.setHash(RectifyChainJudge.hashOf(e));
        RectifyChainJudge.Leg forward = RectifyChainJudge.verifyForward(chain);
        assertFalse(forward.isOk());
        assertTrue(forward.getMessage().contains("时辰"));
    }

    @Test
    void 事后塞笔倒拉念不回来() {
        List<TMgisRectifyEntry> chain = fullChain();
        // 链尾硬塞一笔：序号接着排，前哈希却指空（来路不正）
        TMgisRectifyEntry ghost = new TMgisRectifyEntry();
        ghost.setOrderId(ORDER_ID);
        ghost.setSeq(chain.size() + 1);
        ghost.setRoundNo(2);
        ghost.setStation(RectifyStation.ARCHIVE.name());
        ghost.setAction(RectifyAction.ARCHIVE.name());
        ghost.setPayload("{}");
        ghost.setPennedBy("旁人");
        ghost.setPennedAt(at("2026-09-26 01:00:00"));
        ghost.setPrevHash(RectifyChainJudge.GENESIS);
        ghost.setHash(RectifyChainJudge.hashOf(ghost));
        chain.add(ghost);
        assertFalse(RectifyChainJudge.verifyForward(chain).isOk());
        assertFalse(RectifyChainJudge.verifyBackward(chain).isOk());
    }

    @Test
    void 头一笔不是挂号即来路不正() {
        List<TMgisRectifyEntry> chain = new ArrayList<TMgisRectifyEntry>();
        pen(chain, RectifyAction.NOTIFY, 1, "{}", "张三", "2026-09-20 09:00:00");
        assertFalse(RectifyChainJudge.verifyForward(chain).isOk());
    }

    @Test
    void 空链不合格() {
        assertFalse(RectifyChainJudge.verifyForward(new ArrayList<TMgisRectifyEntry>()).isOk());
        assertFalse(RectifyChainJudge.verifyBackward(new ArrayList<TMgisRectifyEntry>()).isOk());
    }
}
