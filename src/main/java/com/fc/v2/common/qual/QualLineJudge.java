package com.fc.v2.common.qual;

import com.fc.v2.model.auto.TMgisQualLine;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 判定线引擎：处里规矩在系统里的唯一出处。
 *
 * <p>质量监督处定下的口径全部收在这一个类里，核案台、检验机构自查页面、
 * 出结论工序都从这里过，别处不许另写一套——两处口径合不上便是事故。</p>
 *
 * <ul>
 *   <li><b>管不管得着</b>：成果类别合不上、比例尺档落在适用范围之外的，这条线就当没这条线；
 *       适用档空着的线不当通吃。</li>
 *   <li><b>当日谁当家</b>：以交件日为准倒查。启用之日（含）起管，交棒之日（不含）止管——
 *       交棒当日归新线，旧线管到前一日；交棒之日空着即现行。前头日子上的线不许顶上。</li>
 *   <li><b>分档</b>：等号算高一档；越过三档天花板仍归三档；三档留白认作无顶，不许解成断了档。</li>
 *   <li><b>浑数</b>：负数、一长串九之类照原样驳回，休想进账。</li>
 *   <li><b>争档</b>：先比顺位值，大者优先；顺位相同认代号靠后那条。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-09-26
 */
public final class QualLineJudge {

    private QualLineJudge() {
    }

    /** 整数部分连续 9 的个数达到此限，即视为"噼里啪啦敲一长串九"的浑数 */
    public static final int NINE_RUN_LIMIT = 6;

    /**
     * 当家排序：顺位值大者在先；顺位相同，代号靠后（字典序大）者在先。
     * 顺位值未填按 0 论。
     */
    public static final Comparator<TMgisQualLine> OFFICE_ORDER = new Comparator<TMgisQualLine>() {
        @Override
        public int compare(TMgisQualLine a, TMgisQualLine b) {
            int pa = priorityOf(a);
            int pb = priorityOf(b);
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            return cb.compareTo(ca);
        }
    };

    private static int priorityOf(TMgisQualLine line) {
        return line.getPriority() == null ? 0 : line.getPriority();
    }

    /**
     * 头一道工序：这条线管不管得着这份成果。
     * 成果类别合不上、比例尺档落在范围之外，都当没这条线；
     * 适用档空着（上下界缺一）的线不当通吃，什么也管不着。
     */
    public static boolean covers(TMgisQualLine line, String prodType, Integer scaleDenominator) {
        if (line == null || prodType == null || scaleDenominator == null) {
            return false;
        }
        QualProdType want = QualProdType.tryOf(prodType);
        QualProdType has = QualProdType.tryOf(line.getProdType());
        if (want == null || has == null || want != has) {
            return false;
        }
        if (line.getScaleMin() == null || line.getScaleMax() == null) {
            return false;
        }
        return scaleDenominator >= line.getScaleMin() && scaleDenominator <= line.getScaleMax();
    }

    /**
     * 这条线在当日当不当家。
     * 窗口为 [启用之日, 交棒之日)：启用当日即管，交棒当日即不管（当日起归新线）；
     * 交棒之日空着即现行。一律按日历日比，不比钟点。
     */
    public static boolean inOffice(TMgisQualLine line, Date day) {
        if (line == null || day == null || line.getEffStart() == null) {
            return false;
        }
        LocalDate d = toDay(day);
        if (d.isBefore(toDay(line.getEffStart()))) {
            return false;
        }
        return line.getEffEnd() == null || d.isBefore(toDay(line.getEffEnd()));
    }

    /**
     * 从一堆线里筛出当日管得着这份成果的线，按当家规则排好序（最该当家的在头里）。
     */
    public static List<TMgisQualLine> applicable(List<TMgisQualLine> lines, String prodType,
                                                 Integer scaleDenominator, Date day) {
        List<TMgisQualLine> hit = new ArrayList<TMgisQualLine>();
        if (lines != null) {
            for (TMgisQualLine line : lines) {
                if (covers(line, prodType, scaleDenominator) && inOffice(line, day)) {
                    hit.add(line);
                }
            }
        }
        hit.sort(OFFICE_ORDER);
        return hit;
    }

    /**
     * 取当家线：先比顺位值，顺位相同认代号靠后那条。
     * 被挑中的一条要给一句"凭什么是它"，写进结论。
     */
    public static QualPick pick(List<TMgisQualLine> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("候选判定线为空，无从取线");
        }
        List<TMgisQualLine> sorted = new ArrayList<TMgisQualLine>(candidates);
        sorted.sort(OFFICE_ORDER);
        TMgisQualLine top = sorted.get(0);
        String reason;
        if (sorted.size() == 1) {
            reason = "交件日适用判定线仅此一条";
        } else {
            int p = priorityOf(top);
            int tied = 0;
            for (TMgisQualLine l : sorted) {
                if (priorityOf(l) == p) {
                    tied++;
                }
            }
            if (tied <= 1) {
                reason = "交件日适用线共" + sorted.size() + "条，本条顺位值" + p + "最高";
            } else {
                reason = "交件日适用线共" + sorted.size() + "条，最高顺位值" + p + "并列" + tied
                        + "条，本条代号「" + top.getRuleCode() + "」最靠后";
            }
        }
        return new QualPick(top, reason, sorted.size());
    }

    /**
     * 分档：等号算高一档（中误差恰为某档上限，便按高一档说话）；
     * 越过三档天花板的都归三档；三档留白认作无顶，不许解成断了档没人管。
     * 顺带记一句是否恰好压在天花板上。
     */
    public static QualTier tierOf(TMgisQualLine line, BigDecimal meanError) {
        if (line == null || meanError == null) {
            throw new IllegalArgumentException("分档缺线或缺中误差");
        }
        if (line.getTh1Max() == null || line.getTh2Max() == null) {
            throw new IllegalStateException("判定线「" + line.getRuleCode() + "」一、二档天花板未维护，无法分档");
        }
        BigDecimal th1 = line.getTh1Max();
        BigDecimal th2 = line.getTh2Max();
        BigDecimal th3 = line.getTh3Max();
        if (meanError.compareTo(th1) < 0) {
            return new QualTier(1, false);
        }
        if (meanError.compareTo(th1) == 0) {
            // 压一档顶，等号算高一档
            return new QualTier(2, true);
        }
        if (meanError.compareTo(th2) < 0) {
            return new QualTier(2, false);
        }
        if (meanError.compareTo(th2) == 0) {
            // 压二档顶，等号算高一档
            return new QualTier(3, true);
        }
        if (th3 == null) {
            // 三档留白认作无顶
            return new QualTier(3, false);
        }
        if (meanError.compareTo(th3) < 0) {
            return new QualTier(3, false);
        }
        if (meanError.compareTo(th3) == 0) {
            // 压三档顶，上头没有更高档，仍归三档
            return new QualTier(3, true);
        }
        // 越过三档天花板仍归三档
        return new QualTier(3, false);
    }

    /**
     * 中误差校验：填成负数、噼里啪啦敲一长串九，这类浑数照原样驳回，休想进账。
     * 驳回时异常里带填报原值，不许改写。
     *
     * @return 校验过的中误差
     */
    public static BigDecimal parseMeanError(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new QualInputException("中误差未填报", raw);
        }
        String s = raw.trim();
        BigDecimal v;
        try {
            v = new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new QualInputException("中误差不是数，照原样驳回：" + raw, raw);
        }
        if (v.signum() < 0) {
            throw new QualInputException("中误差填成负数，照原样驳回：" + raw, raw);
        }
        String plain = v.abs().toPlainString();
        String intPart = plain.contains(".") ? plain.substring(0, plain.indexOf('.')) : plain;
        if (intPart.matches(".*9{" + NINE_RUN_LIMIT + ",}.*")) {
            throw new QualInputException("中误差疑似一长串九的浑数，照原样驳回：" + raw, raw);
        }
        return v;
    }

    /**
     * 判定全流程：先问哪条线管得着（以交件日倒查），再验中误差，再取当家线分档。
     * 范围里一条都挑不出来，明写当日无可用判定线——前头日子上的线不许顶上。
     *
     * @param lines            候选判定线（调用方负责给未删除的线）
     * @param prodType         成果类别（代号或中文名）
     * @param scaleDenominator 比例尺分母，如 1:2000 传 2000
     * @param submitDay        交件日期（判定以交件日为准，与出结论之日无关）
     * @param meanErrorRaw     中误差填报原值
     */
    public static QualVerdict judge(List<TMgisQualLine> lines, String prodType, Integer scaleDenominator,
                                    Date submitDay, String meanErrorRaw) {
        QualProdType type = QualProdType.require(prodType);
        if (scaleDenominator == null || scaleDenominator <= 0) {
            throw new IllegalArgumentException("比例尺分母不正：" + scaleDenominator);
        }
        if (submitDay == null) {
            throw new IllegalArgumentException("交件日期未填报");
        }
        List<TMgisQualLine> hit = applicable(lines, type.name(), scaleDenominator, submitDay);
        if (hit.isEmpty()) {
            throw new NoQualLineException("当日无可用判定线：" + type.getLabel()
                    + " 1:" + scaleDenominator + " 交件日 " + dayText(submitDay));
        }
        BigDecimal meanError = parseMeanError(meanErrorRaw);
        QualPick pick = pick(hit);
        QualTier tier = tierOf(pick.getLine(), meanError);
        return new QualVerdict(pick.getLine(), tier, meanError, pick.getReason(), pick.getCandidateCount());
    }

    /**
     * 交件日期解析：认 yyyy-MM-dd，也容得下带钟点的写法；认不得即驳回。
     */
    public static Date parseDay(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("交件日期未填报");
        }
        String s = raw.trim();
        String[] patterns = {"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"};
        for (String p : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p);
                f.setLenient(false);
                return f.parse(s);
            } catch (ParseException ignore) {
                // 换下一种写法
            }
        }
        throw new IllegalArgumentException("交件日期格式不识：" + raw);
    }

    /** 日期一律归到日历日再比（java.sql.Date/Timestamp 都走得通） */
    static LocalDate toDay(Date d) {
        return Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    static String dayText(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }
}
