package com.fc.v2.common.catalog;

import com.fc.v2.common.qual.QualProdType;
import com.fc.v2.model.auto.TMgisCatalogLine;
import com.fc.v2.model.custom.CatalogAcceptRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 目录册核收引擎：成果管理处收册规矩在系统里的唯一出处。
 *
 * <p>一册整批递进来，系统逐行解析一遍——行次、图幅代号、成果类别、
 * 属地名址四样跟着每一行走；收下的进账，没收下的挂账，挂账行次、代号、
 * 缘故三样齐着写。服务层、控制器都从这里过，别处不许另写一套。</p>
 *
 * <ul>
 *   <li><b>一处填错不连坐</b>：某行幅数填成负数之类，那一行自己挂着，
 *       缘故写全，其余行照常进账，整册照走。</li>
 *   <li><b>同代号并一条</b>：册里同一个图幅代号占了两行，并成一条进账，
 *       幅数只按头一行加一次——属地交界两侧各挂一次是早年的写法，
 *       账上重复算一遍，全年幅数就凭空多出一截。</li>
 *   <li><b>越线、白表整册不收</b>：行数超过上限一行也不落；零行的白表
 *       不许回一句「已核讫」。</li>
 *   <li><b>三数当场对</b>：挂着几行、进账几行、表上一共几行，
 *       挂着加入账等于表上总行数；核收落账与翻账同经 {@link #tally} 一处算出。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-09-26
 */
public final class CatalogBookJudge {

    private CatalogBookJudge() {
    }

    /** 一册行数上限：越线的整册不收，一行也不落 */
    public static final int MAX_ROWS = 2000;

    /**
     * 整册推演。越线、白表、册号或属地抬头缺失、填报行次册内撞号，
     * 整册顶回（{@link CatalogAcceptException}），一行也不落；
     * 其余局面逐行给结果，收下的进账、没收下的挂账。
     *
     * @param req 整批提交请求（只带填报原值，收不收系统说了算）
     * @return 一册的推演定局：逐行结果加三数对账
     */
    public static CatalogVerdict judge(CatalogAcceptRequest req) {
        if (req == null) {
            throw new CatalogAcceptException("册子未递到：整批提交请求为空");
        }
        String bookNo = trim(req.getBookNo());
        if (bookNo == null) {
            throw new CatalogAcceptException("册号缺失：一册一号，没册号没法落账，整册不收");
        }
        String submitRegion = trim(req.getSubmitRegion());
        if (submitRegion == null) {
            throw new CatalogAcceptException("递交属地缺失：册子抬头没写是哪个市递的，整册不收");
        }
        List<CatalogAcceptRequest.Line> raw = req.getLines();
        if (raw == null || raw.isEmpty()) {
            // 零行的白表不许回「已核讫」——市里据此以为交完了，年底两头账各说各话
            throw new CatalogAcceptException("一张白纸：册上一行也没有，不许回「已核讫」，整册不收");
        }
        if (raw.size() > MAX_ROWS) {
            throw new CatalogAcceptException("行数越线：表上 " + raw.size() + " 行，越过上限 " + MAX_ROWS
                    + " 行，整册不收，一行也不落");
        }

        int[] rowNos = assignRowNos(raw);

        // 逐行核：一处填错只挂这一行，不连坐
        List<CatalogLineOutcome> lines = new ArrayList<CatalogLineOutcome>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            lines.add(judgeLine(rowNos[i], raw.get(i)));
        }

        // 同代号并一条：进账行里同一个图幅代号占两行的，并成一条，
        // 幅数只按头一行加一次；被并的那行仍算进账行，头行行次指过去
        Map<String, Integer> primaryByCode = new LinkedHashMap<String, Integer>();
        List<CatalogLineOutcome> merged = new ArrayList<CatalogLineOutcome>(lines.size());
        for (CatalogLineOutcome line : lines) {
            Integer primary = null;
            if (line.isAccepted()) {
                Integer seen = primaryByCode.get(line.getSheetCode());
                if (seen == null) {
                    primaryByCode.put(line.getSheetCode(), line.getRowNo());
                    primary = line.getRowNo();
                } else {
                    primary = seen;
                }
            }
            merged.add(new CatalogLineOutcome(line.getRowNo(), line.getSheetCode(), line.getProdType(),
                    line.getRegion(), line.getSheetCountRaw(), line.getSheetCount(), line.getDisposition(),
                    line.getReasonCode(), line.getReasonText(), primary));
        }

        return new CatalogVerdict(bookNo, submitRegion, merged, tallyOf(merged));
    }

    /**
     * 三数对账：挂着几行、进账几行、表上一共几行，当场对得上。
     * 核收落账与凭册号、凭属地翻账同经这一处算出——回执上的数字
     * 与库里的数字出自同一处计算。
     */
    public static CatalogTally tally(List<TMgisCatalogLine> lines) {
        int total = 0;
        int accepted = 0;
        int pending = 0;
        int entries = 0;
        int sheetTotal = 0;
        if (lines != null) {
            for (TMgisCatalogLine line : lines) {
                total++;
                if (CatalogDisposition.ACCEPTED.name().equals(line.getDisposition())) {
                    accepted++;
                    if (line.getPrimaryRowNo() != null && line.getPrimaryRowNo().equals(line.getRowNo())) {
                        entries++;
                        sheetTotal += line.getSheetCount() == null ? 0 : line.getSheetCount();
                    }
                } else {
                    pending++;
                }
            }
        }
        return new CatalogTally(total, accepted, pending, entries, sheetTotal);
    }

    /** 推演结果口径的三数（与 {@link #tally} 同一算法，落账前用） */
    private static CatalogTally tallyOf(List<CatalogLineOutcome> lines) {
        int accepted = 0;
        int pending = 0;
        int entries = 0;
        int sheetTotal = 0;
        for (CatalogLineOutcome line : lines) {
            if (line.isAccepted()) {
                accepted++;
                if (line.isPrimary()) {
                    entries++;
                    sheetTotal += line.getSheetCount() == null ? 0 : line.getSheetCount();
                }
            } else {
                pending++;
            }
        }
        return new CatalogTally(lines.size(), accepted, pending, entries, sheetTotal);
    }

    /**
     * 核一行。图幅代号、成果类别、属地名址、幅数逐项过，命中的缘故
     * 齐着写全；一项不犯才进账。幅数照填报原值核：不是数、负数都挂，
     * 原值留在缘故里，追得到原笔。
     */
    private static CatalogLineOutcome judgeLine(int rowNo, CatalogAcceptRequest.Line raw) {
        String sheetCode = trim(raw.getSheetCode());
        String prodTypeRaw = trim(raw.getProdType());
        String region = trim(raw.getRegion());
        String sheetCountRaw = raw.getSheetCount() == null ? null : raw.getSheetCount().trim();

        List<CatalogRejectReason> reasons = new ArrayList<CatalogRejectReason>(2);
        if (sheetCode == null) {
            reasons.add(CatalogRejectReason.MISSING_SHEET_CODE);
        }
        QualProdType prodType = null;
        if (prodTypeRaw == null) {
            reasons.add(CatalogRejectReason.MISSING_PROD_TYPE);
        } else {
            prodType = QualProdType.tryOf(prodTypeRaw);
            if (prodType == null) {
                reasons.add(CatalogRejectReason.UNSUPPORTED_PROD_TYPE);
            }
        }
        if (region == null) {
            reasons.add(CatalogRejectReason.MISSING_REGION);
        }
        Integer sheetCount = parseSheetCount(sheetCountRaw, reasons);

        if (!reasons.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (CatalogRejectReason reason : reasons) {
                if (text.length() > 0) {
                    text.append('；');
                }
                text.append(reason.getLabel());
                if (reason == CatalogRejectReason.NEGATIVE_SHEET_COUNT
                        || reason == CatalogRejectReason.SHEET_COUNT_NOT_A_NUMBER) {
                    // 浑数照原样驳回：缘故里带上填报原值，追得到原笔
                    text.append("（填报原值：").append(sheetCountRaw == null ? "" : sheetCountRaw).append('）');
                }
            }
            return new CatalogLineOutcome(rowNo, sheetCode,
                    prodType != null ? prodType.name() : prodTypeRaw, region,
                    sheetCountRaw, null, CatalogDisposition.PENDING,
                    reasons.get(0).getCode(), text.toString(), null);
        }
        return new CatalogLineOutcome(rowNo, sheetCode, prodType.name(), region,
                sheetCountRaw, sheetCount, CatalogDisposition.ACCEPTED, null, null, null);
    }

    /**
     * 幅数照填报原值核：空白、写字、带小数之类不是数；负数挂账。
     * 不是数、为负都记进缘故，返回 null；过得去返回解析值。
     */
    private static Integer parseSheetCount(String raw, List<CatalogRejectReason> reasons) {
        if (raw == null || raw.isEmpty()) {
            reasons.add(CatalogRejectReason.SHEET_COUNT_NOT_A_NUMBER);
            return null;
        }
        long value;
        try {
            // 只认整数：带小数、带字母、带符号以外的字符都不是数
            if (!raw.matches("[+-]?\\d+")) {
                throw new NumberFormatException(raw);
            }
            value = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            reasons.add(CatalogRejectReason.SHEET_COUNT_NOT_A_NUMBER);
            return null;
        }
        if (value < 0) {
            reasons.add(CatalogRejectReason.NEGATIVE_SHEET_COUNT);
            return null;
        }
        if (value > Integer.MAX_VALUE) {
            reasons.add(CatalogRejectReason.SHEET_COUNT_NOT_A_NUMBER);
            return null;
        }
        return (int) value;
    }

    /**
     * 行次归置：填报所指的先用，未填的按原表次序补空号（自一连排）。
     * 填报行次册内撞号、或不是正整数——行次对不上原表，挂账缘故就没法
     * 指到具体哪一行，整册不收。
     */
    private static int[] assignRowNos(List<CatalogAcceptRequest.Line> raw) {
        int n = raw.size();
        int[] rowNos = new int[n];
        Set<Integer> taken = new HashSet<Integer>();
        for (int i = 0; i < n; i++) {
            Integer claimed = raw.get(i).getRowNo();
            if (claimed != null) {
                if (claimed <= 0 || !taken.add(claimed)) {
                    throw new CatalogAcceptException("行次对不上原表：第 " + (i + 1) + " 行填报行次 "
                            + claimed + " 册内撞号或不正，整册不收");
                }
                rowNos[i] = claimed;
            }
        }
        int next = 1;
        for (int i = 0; i < n; i++) {
            if (raw.get(i).getRowNo() == null) {
                while (taken.contains(next)) {
                    next++;
                }
                rowNos[i] = next;
                taken.add(next);
            }
        }
        return rowNos;
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
