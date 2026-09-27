package com.fc.v2.qual;

import com.fc.v2.common.qual.NoQualLineException;
import com.fc.v2.common.qual.QualInputException;
import com.fc.v2.mapper.auto.TMgisQualConclusionMapper;
import com.fc.v2.mapper.auto.TMgisQualLineMapper;
import com.fc.v2.model.auto.TMgisQualConclusion;
import com.fc.v2.model.auto.TMgisQualLine;
import com.fc.v2.model.custom.QualIssueRequest;
import com.fc.v2.service.impl.TMgisQualConclusionServiceImpl;
import com.fc.v2.service.impl.TMgisQualLineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 出结论工序单测：快照落账、浑数不进账、无可用线不进账、两栏与出结论日无关。
 */
class TMgisQualConclusionServiceImplTest {

    private TMgisQualLineMapper lineMapper;
    private TMgisQualConclusionMapper conclusionMapper;
    private TMgisQualConclusionServiceImpl conclusionService;
    private List<TMgisQualLine> lines;

    @BeforeEach
    void setUp() {
        lineMapper = mock(TMgisQualLineMapper.class);
        conclusionMapper = mock(TMgisQualConclusionMapper.class);
        TMgisQualLineServiceImpl lineService = new TMgisQualLineServiceImpl();
        ReflectionTestUtils.setField(lineService, "mgisQualLineMapper", lineMapper);
        conclusionService = new TMgisQualConclusionServiceImpl();
        ReflectionTestUtils.setField(conclusionService, "mgisQualConclusionMapper", conclusionMapper);
        ReflectionTestUtils.setField(conclusionService, "mgisQualLineService", lineService);

        lines = new ArrayList<TMgisQualLine>(Arrays.asList(
                line(11L, "JX-2019", 1, "2019-01-01", "2021-01-01"),
                line(22L, "JX-2021", 1, "2021-01-01", null)));
        when(lineMapper.selectList(any())).thenReturn(lines);
        when(conclusionMapper.insert(any(TMgisQualConclusion.class))).thenReturn(1);
    }

    private static TMgisQualLine line(Long id, String code, int priority, String start, String end) {
        TMgisQualLine l = new TMgisQualLine();
        l.setId(id);
        l.setRuleCode(code);
        l.setRuleName("线-" + code);
        l.setPriority(priority);
        l.setEffStart(day(start));
        l.setEffEnd(end == null ? null : day(end));
        l.setTh1Max(new BigDecimal("0.5"));
        l.setTh2Max(new BigDecimal("1.0"));
        l.setTh3Max(new BigDecimal("2.0"));
        l.setProdType("TOPO_MAP");
        l.setScaleMin(500);
        l.setScaleMax(2000);
        l.setDelFlag(0);
        return l;
    }

    private static Date day(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static QualIssueRequest req(String itemCode, String submitDate, String meanError) {
        QualIssueRequest r = new QualIssueRequest();
        r.setItemCode(itemCode);
        r.setProdType("TOPO_MAP");
        r.setScaleDenominator(2000);
        r.setSubmitDate(submitDate);
        r.setMeanError(meanError);
        return r;
    }

    @Test
    void 出结论把代号与启用之日快照落账() {
        TMgisQualConclusion c = conclusionService.issue(req("MAP-2019-001", "2019-06-01", "0.4"));
        // 2019 年交件，用当年当家的老线
        assertEquals("JX-2019", c.getRuleCode());
        assertEquals(day("2019-01-01"), c.getRuleEffStart());
        assertEquals("线-JX-2019", c.getRuleName());
        assertEquals(Long.valueOf(11L), c.getRuleId());
        assertEquals(Integer.valueOf(1), c.getTier());
        assertEquals(Integer.valueOf(0), c.getOnCeiling());
        assertEquals(new BigDecimal("0.4"), c.getMeanError());
        assertNotNull(c.getPickReason());
        assertNotNull(c.getIssuedAt());

        ArgumentCaptor<TMgisQualConclusion> captor = ArgumentCaptor.forClass(TMgisQualConclusion.class);
        verify(conclusionMapper, times(1)).insert(captor.capture());
        assertEquals("JX-2019", captor.getValue().getRuleCode());
    }

    @Test
    void 同一幅图今天出与明天出_两栏一个字不差() {
        TMgisQualConclusion first = conclusionService.issue(req("MAP-2019-001", "2019-06-01", "0.4"));
        // 隔些日子，桌上换了新线，同一幅图再出一次
        TMgisQualConclusion second = conclusionService.issue(req("MAP-2019-001", "2019-06-01", "0.4"));
        assertEquals(first.getRuleCode(), second.getRuleCode());
        assertEquals(first.getRuleEffStart(), second.getRuleEffStart());
    }

    @Test
    void 今年新交件的图_已让位老线不认() {
        TMgisQualConclusion c = conclusionService.issue(req("MAP-2026-001", "2026-05-01", "0.4"));
        assertEquals("JX-2021", c.getRuleCode());
        assertEquals(day("2021-01-01"), c.getRuleEffStart());
    }

    @Test
    void 浑数驳回_休想进账() {
        assertThrows(QualInputException.class,
                () -> conclusionService.issue(req("MAP-X", "2019-06-01", "-1")));
        assertThrows(QualInputException.class,
                () -> conclusionService.issue(req("MAP-X", "2019-06-01", "999999999")));
        verify(conclusionMapper, never()).insert(any());
    }

    @Test
    void 当日无可用判定线_明写且不进账() {
        NoQualLineException e = assertThrows(NoQualLineException.class,
                () -> conclusionService.issue(req("MAP-X", "2015-06-01", "0.4")));
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("当日无可用判定线"));
        verify(conclusionMapper, never()).insert(any());
    }

    @Test
    void 压线结论记压线() {
        // 中误差恰为二档上限，按三档说话，记压线
        TMgisQualConclusion c = conclusionService.issue(req("MAP-Y", "2026-05-01", "1.0"));
        assertEquals(Integer.valueOf(3), c.getTier());
        assertEquals(Integer.valueOf(1), c.getOnCeiling());
    }
}
