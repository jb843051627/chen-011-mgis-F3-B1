package com.fc.v2.qual;

import com.fc.v2.common.qual.NoQualLineException;
import com.fc.v2.common.qual.QualInputException;
import com.fc.v2.common.qual.QualLineJudge;
import com.fc.v2.common.qual.QualPick;
import com.fc.v2.common.qual.QualTier;
import com.fc.v2.common.qual.QualVerdict;
import com.fc.v2.model.auto.TMgisQualLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判定线引擎单测：处里规矩逐条对号。
 */
class QualLineJudgeTest {

    private static TMgisQualLine line(String code, int priority, String start, String end,
                                      String th1, String th2, String th3,
                                      String prodType, Integer scaleMin, Integer scaleMax) {
        TMgisQualLine l = new TMgisQualLine();
        l.setRuleCode(code);
        l.setRuleName("线-" + code);
        l.setPriority(priority);
        l.setEffStart(day(start));
        l.setEffEnd(end == null ? null : day(end));
        l.setTh1Max(new BigDecimal(th1));
        l.setTh2Max(new BigDecimal(th2));
        l.setTh3Max(th3 == null ? null : new BigDecimal(th3));
        l.setProdType(prodType);
        l.setScaleMin(scaleMin);
        l.setScaleMax(scaleMax);
        return l;
    }

    /** 地形图 1:500~1:2000 常用线：0.5 / 1.0 / 2.0 */
    private static TMgisQualLine topoLine(String code, int priority, String start, String end) {
        return line(code, priority, start, end, "0.5", "1.0", "2.0", "TOPO_MAP", 500, 2000);
    }

    private static Date day(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // ---------- 头一道工序：管不管得着 ----------

    @Test
    void 类别不合的线管不着() {
        TMgisQualLine imageLine = line("IMG-01", 1, "2020-01-01", null,
                "0.5", "1.0", "2.0", "IMAGE_MAP", 500, 2000);
        assertFalse(QualLineJudge.covers(imageLine, "TOPO_MAP", 2000));
        // 中文名也认得
        assertTrue(QualLineJudge.covers(topoLine("JX-01", 1, "2020-01-01", null), "地形图", 2000));
    }

    @Test
    void 比例尺档落在范围之外管不着() {
        TMgisQualLine l = topoLine("JX-01", 1, "2020-01-01", null);
        assertFalse(QualLineJudge.covers(l, "TOPO_MAP", 5000));
        assertFalse(QualLineJudge.covers(l, "TOPO_MAP", 100));
        // 档界含头含尾
        assertTrue(QualLineJudge.covers(l, "TOPO_MAP", 500));
        assertTrue(QualLineJudge.covers(l, "TOPO_MAP", 2000));
    }

    @Test
    void 适用档空着的线不当通吃() {
        TMgisQualLine l = line("JX-空档", 1, "2020-01-01", null,
                "0.5", "1.0", "2.0", "TOPO_MAP", null, null);
        assertFalse(QualLineJudge.covers(l, "TOPO_MAP", 2000));
        // 上下界缺一也算空着
        TMgisQualLine half = line("JX-半档", 1, "2020-01-01", null,
                "0.5", "1.0", "2.0", "TOPO_MAP", 500, null);
        assertFalse(QualLineJudge.covers(half, "TOPO_MAP", 2000));
    }

    // ---------- 当日谁当家 ----------

    @Test
    void 交棒当日旧线不管新线管() {
        TMgisQualLine oldLine = topoLine("JX-2019", 1, "2019-01-01", "2020-01-01");
        TMgisQualLine newLine = topoLine("JX-2020", 1, "2020-01-01", null);
        assertTrue(QualLineJudge.inOffice(oldLine, day("2019-12-31")));
        assertFalse(QualLineJudge.inOffice(oldLine, day("2020-01-01")));
        assertTrue(QualLineJudge.inOffice(newLine, day("2020-01-01")));
        assertFalse(QualLineJudge.inOffice(newLine, day("2019-12-31")));
    }

    @Test
    void 当日无可用判定线_前头日子的不许顶上() {
        TMgisQualLine retired = topoLine("JX-2017", 1, "2017-01-01", "2018-01-01");
        NoQualLineException e = assertThrows(NoQualLineException.class, () ->
                QualLineJudge.judge(Collections.singletonList(retired),
                        "TOPO_MAP", 2000, day("2019-06-01"), "0.4"));
        assertTrue(e.getMessage().contains("当日无可用判定线"));
    }

    @Test
    void 一九年交件的图今年讨结论_用交件日当家的老线() {
        TMgisQualLine oldLine = topoLine("JX-2019", 1, "2019-01-01", "2021-01-01");
        TMgisQualLine newLine = topoLine("JX-2021", 1, "2021-01-01", null);
        List<TMgisQualLine> lines = Arrays.asList(oldLine, newLine);
        // 2019 年交件：老线作数，哪怕它早已让位
        QualVerdict v2019 = QualLineJudge.judge(lines, "TOPO_MAP", 2000, day("2019-06-01"), "0.4");
        assertEquals("JX-2019", v2019.getLine().getRuleCode());
        // 今年新交件：已让位的老线一概不认
        QualVerdict v2026 = QualLineJudge.judge(lines, "TOPO_MAP", 2000, day("2026-05-01"), "0.4");
        assertEquals("JX-2021", v2026.getLine().getRuleCode());
    }

    // ---------- 分档 ----------

    @Test
    void 等号算高一档() {
        TMgisQualLine l = topoLine("JX-01", 1, "2020-01-01", null);
        assertEquals(1, QualLineJudge.tierOf(l, new BigDecimal("0.49")).getTier());
        // 恰为一档上限，按二档说话
        QualTier t = QualLineJudge.tierOf(l, new BigDecimal("0.5"));
        assertEquals(2, t.getTier());
        assertTrue(t.isOnCeiling());
        assertEquals(2, QualLineJudge.tierOf(l, new BigDecimal("0.7")).getTier());
        // 恰为二档上限，按三档说话
        t = QualLineJudge.tierOf(l, new BigDecimal("1.0"));
        assertEquals(3, t.getTier());
        assertTrue(t.isOnCeiling());
    }

    @Test
    void 越过三档天花板仍归三档() {
        TMgisQualLine l = topoLine("JX-01", 1, "2020-01-01", null);
        assertEquals(3, QualLineJudge.tierOf(l, new BigDecimal("1.5")).getTier());
        // 恰压三档顶：归三档，记压线
        QualTier t = QualLineJudge.tierOf(l, new BigDecimal("2.0"));
        assertEquals(3, t.getTier());
        assertTrue(t.isOnCeiling());
        // 越过三档顶：仍归三档，不算压线
        t = QualLineJudge.tierOf(l, new BigDecimal("2.5"));
        assertEquals(3, t.getTier());
        assertFalse(t.isOnCeiling());
    }

    @Test
    void 三档留白认作无顶() {
        TMgisQualLine legacy = line("JX-老线", 1, "2019-01-01", null,
                "0.5", "1.0", null, "TOPO_MAP", 500, 2000);
        QualTier t = QualLineJudge.tierOf(legacy, new BigDecimal("99"));
        assertEquals(3, t.getTier());
        assertFalse(t.isOnCeiling());
    }

    @Test
    void 压线与否照实回话() {
        TMgisQualLine l = topoLine("JX-01", 1, "2020-01-01", null);
        assertFalse(QualLineJudge.tierOf(l, new BigDecimal("0.49")).isOnCeiling());
        assertTrue(QualLineJudge.tierOf(l, new BigDecimal("0.5")).isOnCeiling());
        assertFalse(QualLineJudge.tierOf(l, new BigDecimal("0.51")).isOnCeiling());
    }

    // ---------- 浑数 ----------

    @Test
    void 负数照原样驳回() {
        QualInputException e = assertThrows(QualInputException.class,
                () -> QualLineJudge.parseMeanError("-0.1"));
        assertEquals("-0.1", e.getRawInput());
    }

    @Test
    void 一长串九照原样驳回() {
        assertThrows(QualInputException.class, () -> QualLineJudge.parseMeanError("999999"));
        assertThrows(QualInputException.class, () -> QualLineJudge.parseMeanError("99999999.5"));
        assertThrows(QualInputException.class, () -> QualLineJudge.parseMeanError("12999999"));
        QualInputException e = assertThrows(QualInputException.class,
                () -> QualLineJudge.parseMeanError("9999999"));
        assertEquals("9999999", e.getRawInput());
        // 五个九不是一长串；小数点后的九不算浑数
        assertEquals(new BigDecimal("99999"), QualLineJudge.parseMeanError("99999"));
        assertEquals(new BigDecimal("0.999999"), QualLineJudge.parseMeanError("0.999999"));
    }

    @Test
    void 不是数照原样驳回() {
        assertThrows(QualInputException.class, () -> QualLineJudge.parseMeanError("abc"));
        assertThrows(QualInputException.class, () -> QualLineJudge.parseMeanError(""));
        assertThrows(QualInputException.class, () -> QualLineJudge.parseMeanError(null));
    }

    // ---------- 争档 ----------

    @Test
    void 争档先比顺位值() {
        TMgisQualLine lo = topoLine("JX-低", 3, "2020-01-01", null);
        TMgisQualLine hi = topoLine("JX-高", 5, "2020-01-01", null);
        QualPick pick = QualLineJudge.pick(Arrays.asList(lo, hi));
        assertEquals("JX-高", pick.getLine().getRuleCode());
        assertTrue(pick.getReason().contains("顺位值5最高"));
    }

    @Test
    void 顺位相同认代号靠后() {
        TMgisQualLine a = topoLine("JX-01", 5, "2020-01-01", null);
        TMgisQualLine b = topoLine("JX-02", 5, "2020-01-01", null);
        QualPick pick = QualLineJudge.pick(Arrays.asList(a, b));
        assertEquals("JX-02", pick.getLine().getRuleCode());
        assertTrue(pick.getReason().contains("代号"));
        assertTrue(pick.getReason().contains("JX-02"));
    }

    @Test
    void 仅此一条照实说() {
        QualPick pick = QualLineJudge.pick(Collections.singletonList(topoLine("JX-独", 1, "2020-01-01", null)));
        assertEquals("JX-独", pick.getLine().getRuleCode());
        assertTrue(pick.getReason().contains("仅此一条"));
    }

    // ---------- 全流程与确定性 ----------

    @Test
    void 全流程_适用线之外的不来搅局() {
        List<TMgisQualLine> lines = new ArrayList<TMgisQualLine>();
        lines.add(topoLine("JX-地形", 1, "2020-01-01", null));
        lines.add(line("JX-影像", 9, "2020-01-01", null, "0.5", "1.0", "2.0", "IMAGE_MAP", 500, 2000));
        lines.add(line("JX-大比例", 9, "2020-01-01", null, "0.5", "1.0", "2.0", "TOPO_MAP", 10000, 25000));
        // 影像线、大比例尺线顺位再高也轮不到它们
        QualVerdict v = QualLineJudge.judge(lines, "TOPO_MAP", 2000, day("2026-01-01"), "0.4");
        assertEquals("JX-地形", v.getLine().getRuleCode());
        assertEquals(1, v.getTier());
    }

    @Test
    void 结论两栏与出结论之日无关() {
        List<TMgisQualLine> lines = Arrays.asList(
                topoLine("JX-2019", 1, "2019-01-01", "2021-01-01"),
                topoLine("JX-2021", 1, "2021-01-01", null));
        // 同一幅图、同一交件日，今天出与明天出，判定依据一字不差
        QualVerdict today = QualLineJudge.judge(lines, "TOPO_MAP", 2000, day("2019-06-01"), "0.4");
        QualVerdict tomorrow = QualLineJudge.judge(lines, "TOPO_MAP", 2000, day("2019-06-01"), "0.4");
        assertEquals(today.getLine().getRuleCode(), tomorrow.getLine().getRuleCode());
        assertEquals(today.getLine().getEffStart(), tomorrow.getLine().getEffStart());
    }

    @Test
    void 歪参数驳回() {
        List<TMgisQualLine> lines = Collections.singletonList(topoLine("JX-01", 1, "2020-01-01", null));
        assertThrows(IllegalArgumentException.class,
                () -> QualLineJudge.judge(lines, "不存在", 2000, day("2026-01-01"), "0.4"));
        assertThrows(IllegalArgumentException.class,
                () -> QualLineJudge.judge(lines, "TOPO_MAP", 0, day("2026-01-01"), "0.4"));
        assertThrows(IllegalArgumentException.class,
                () -> QualLineJudge.judge(lines, "TOPO_MAP", 2000, null, "0.4"));
    }
}
