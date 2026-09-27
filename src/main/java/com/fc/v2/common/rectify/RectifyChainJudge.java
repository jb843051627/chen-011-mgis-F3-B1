package com.fc.v2.common.rectify;

import com.fc.v2.model.auto.TMgisRectifyEntry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 落笔链校验：封存之后，整张单顺着落笔先后拉一遍、倒着也拉一遍。
 *
 * <p>每一笔记「经办、时辰、写的话、前一笔的哈希」，落笔那一下由系统写死，
 * 事后由不得人回头补录，也不许挪时辰。顺拉从挂号念到封存，逐笔重算哈希、
 * 逐笔对前一笔的接扣，须与当日逐站页面上留下的内容一字不差；倒拉从封存
 * 沿前一笔的哈希念回挂号，一笔都不能少。少一笔、次序错一处，或者哪一笔
 * 系事后人手补进去的（哈希对不上、时辰倒挂），这批整改就算不合格。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
public final class RectifyChainJudge {

    private RectifyChainJudge() {
    }

    /** 头一笔的前哈希：64 个 0，挂号那一笔认它 */
    public static final String GENESIS =
            "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * 本笔哈希：把这一笔的站、动作、写的话、经办、落笔时辰、前一笔哈希
     * 一并押进 SHA-256。动其中任何一处，哈希即对不上——事后补录、改话、
     * 挪时辰都藏不住。
     */
    public static String hashOf(TMgisRectifyEntry e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getOrderId()).append('\n')
                .append(e.getSeq()).append('\n')
                .append(e.getRoundNo()).append('\n')
                .append(e.getStation()).append('\n')
                .append(e.getAction()).append('\n')
                .append(e.getPayload() == null ? "" : e.getPayload()).append('\n')
                .append(e.getPennedBy() == null ? "" : e.getPennedBy()).append('\n')
                .append(e.getPennedAt() == null ? "" : penText(e)).append('\n')
                .append(e.getPrevHash());
        return sha256Hex(sb.toString());
    }

    /**
     * 顺拉：从挂号念到封存。
     * 序号自 1 连排、头一笔是挂号且认 GENESIS、每笔接得住前一笔的哈希、
     * 内容重算一字不差、时辰只增不减、走站次序合法——哪一处对不上即不合格。
     *
     * @param entries 按 seq 升序排好的全链
     */
    public static Leg verifyForward(List<TMgisRectifyEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Leg.fail(null, "链上一笔都没有，挂号那一笔不见了");
        }
        TMgisRectifyEntry prev = null;
        int expectSeq = 1;
        for (TMgisRectifyEntry cur : entries) {
            Long at = cur.getSeq() == null ? null : cur.getSeq().longValue();
            if (cur.getSeq() == null || cur.getSeq() != expectSeq) {
                return Leg.fail(at, "第 " + expectSeq + " 笔序号断档或错位，链上少笔或次序错乱");
            }
            if (expectSeq == 1) {
                if (!RectifyAction.REGISTER.name().equals(cur.getAction())) {
                    return Leg.fail(at, "头一笔不是挂号，来路不正");
                }
                if (!GENESIS.equals(cur.getPrevHash())) {
                    return Leg.fail(at, "挂号那一笔的前哈希不是 64 个 0，来路不正");
                }
            } else {
                if (!prev.getHash().equals(cur.getPrevHash())) {
                    return Leg.fail(at, "第 " + expectSeq + " 笔接不住前一笔，链在这里断开");
                }
                if (prev.getPennedAt() != null && cur.getPennedAt() != null
                        && cur.getPennedAt().before(prev.getPennedAt())) {
                    return Leg.fail(at, "第 " + expectSeq + " 笔时辰早于前一笔，疑似事后挪时辰补录");
                }
                if (!RectifyFlow.legalTransition(prev, cur)) {
                    return Leg.fail(at, "第 " + expectSeq + " 笔走站次序错乱："
                            + RectifyFlow.labelOf(prev.getStation()) + " → "
                            + RectifyFlow.labelOf(cur.getStation()) + " 不合章程");
                }
            }
            if (cur.getHash() == null || !cur.getHash().equals(hashOf(cur))) {
                return Leg.fail(at, "第 " + expectSeq + " 笔内容被改动（哈希对不上），疑似事后人手补录");
            }
            prev = cur;
            expectSeq++;
        }
        return Leg.ok();
    }

    /**
     * 倒拉：从封存沿前一笔的哈希念回挂号。
     * 每一步的前哈希都须在链上找得到且只找得到一笔，念到头须是挂号，
     * 步数须与链上总笔数相合——少一笔即不合格。
     *
     * @param entries 按 seq 升序排好的全链
     */
    public static Leg verifyBackward(List<TMgisRectifyEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Leg.fail(null, "链上一笔都没有，无从倒拉");
        }
        Map<String, TMgisRectifyEntry> byHash = new HashMap<String, TMgisRectifyEntry>();
        for (TMgisRectifyEntry e : entries) {
            if (e.getHash() != null && byHash.put(e.getHash(), e) != null) {
                return Leg.fail(e.getSeq() == null ? null : e.getSeq().longValue(),
                        "两笔同一个哈希，链上有鬼");
            }
        }
        TMgisRectifyEntry cur = entries.get(entries.size() - 1);
        int steps = 1;
        while (!GENESIS.equals(cur.getPrevHash())) {
            TMgisRectifyEntry prev = byHash.get(cur.getPrevHash());
            if (prev == null) {
                return Leg.fail(cur.getSeq() == null ? null : cur.getSeq().longValue(),
                        "倒拉断在第 " + cur.getSeq() + " 笔：前一笔找不到，链上少笔");
            }
            cur = prev;
            steps++;
        }
        if (!RectifyAction.REGISTER.name().equals(cur.getAction())) {
            return Leg.fail(cur.getSeq() == null ? null : cur.getSeq().longValue(),
                    "倒拉念到头不是挂号，来路不正");
        }
        if (steps != entries.size()) {
            return Leg.fail(null, "倒拉只念回 " + steps + " 笔，链上共 " + entries.size()
                    + " 笔，有笔不在链上（疑似事后塞入）");
        }
        return Leg.ok();
    }

    /** 落笔时辰一律精确到秒（库面 DATETIME 以秒计，落笔与校验同一口径） */
    static String penText(TMgisRectifyEntry e) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(e.getPennedAt());
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不在台上", e);
        }
    }

    /**
     * 一程校验的结论：顺拉一程、倒拉一程，各自回话。
     */
    public static final class Leg {
        private final boolean ok;
        private final Long breakSeq;
        private final String message;

        private Leg(boolean ok, Long breakSeq, String message) {
            this.ok = ok;
            this.breakSeq = breakSeq;
            this.message = message;
        }

        public static Leg ok() {
            return new Leg(true, null, "通");
        }

        public static Leg fail(Long breakSeq, String message) {
            return new Leg(false, breakSeq, message);
        }

        public boolean isOk() {
            return ok;
        }

        /** 断在第几笔（链上序号），不断为 null */
        public Long getBreakSeq() {
            return breakSeq;
        }

        public String getMessage() {
            return message;
        }
    }
}
