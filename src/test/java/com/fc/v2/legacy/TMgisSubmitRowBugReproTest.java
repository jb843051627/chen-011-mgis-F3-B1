package com.fc.v2.legacy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMgisSubmitRowMapper;
import com.fc.v2.model.auto.TMgisSubmitRow;
import com.fc.v2.service.impl.TMgisSubmitRowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 成果目录汇交整批核收——老通道（TMgisSubmitRowServiceImpl）毛病复现。
 *
 * <p>正确口径以 doc/mgis-catalog-book.md 为准（同摊业务的新门
 * POST /mgisCatalog/accept 已照此实现）：空批平稳托住、越线整册不收、
 * 坏行只挂自己不连坐、行号对齐原表、重样明细并一条、重递幂等、
 * 回执口径与落账同源。以下用例在当前老实现下全部应红。</p>
 */
class TMgisSubmitRowBugReproTest {

    private TMgisSubmitRowMapper mapper;
    private TMgisSubmitRowServiceImpl service;
    private final List<TMgisSubmitRow> store = new ArrayList<TMgisSubmitRow>();

    @BeforeEach
    void setUp() {
        mapper = mock(TMgisSubmitRowMapper.class);
        service = new TMgisSubmitRowServiceImpl();
        ReflectionTestUtils.setField(service, "mgisSubmitRowMapper", mapper);
        when(mapper.insert(any())).thenAnswer(inv -> {
            store.add(inv.getArgument(0));
            return 1;
        });
        when(mapper.selectList(any())).thenAnswer(inv -> {
            List<TMgisSubmitRow> fail = new ArrayList<TMgisSubmitRow>();
            for (TMgisSubmitRow r : store) {
                if (r.getStatus() != null && r.getStatus() == 2) {
                    fail.add(r);
                }
            }
            return fail;
        });
    }

    private static TMgisSubmitRow row(String batchNo, String code, String qty) {
        TMgisSubmitRow r = new TMgisSubmitRow();
        r.setBatchNo(batchNo);
        r.setItemCode(code);
        r.setQty(qty == null ? null : new BigDecimal(qty));
        return r;
    }

    /** 空批、null 批：平稳托住，返回 0，一笔不落，不许抛错 */
    @Test
    void 递进来一个空批平稳托住而不是抛错收场() {
        assertDoesNotThrow(() -> assertEquals(0, service.submitBatch("B0", new ArrayList<TMgisSubmitRow>())));
        assertDoesNotThrow(() -> assertEquals(0, service.submitBatch("B0", null)));
        assertTrue(store.isEmpty());
    }

    /** 行数越过上限（500）：整册不收，一行也不落 */
    @Test
    void 行数越过上限整册不收一行也不落() {
        List<TMgisSubmitRow> many = new ArrayList<TMgisSubmitRow>();
        for (int i = 1; i <= 501; i++) {
            many.add(row("BIG", "CODE" + i, "1"));
        }
        int accepted = service.submitBatch("BIG", many);
        assertEquals(0, accepted);
        assertTrue(store.isEmpty(), "越线册子一笔都不该落，实落 " + store.size());
    }

    /** 一两行坏珠不连坐：坏行挂账，合格行照常进账；回执进账数=实际进账行数 */
    @Test
    void 坏行只挂自己合格行照常进账不连坐() {
        List<TMgisSubmitRow> rows = new ArrayList<TMgisSubmitRow>();
        rows.add(row("B1", "A1", "10"));   // 合格
        rows.add(row("B1", "A2", null));   // 坏：幅数空
        rows.add(row("B1", "A3", "5"));    // 合格
        int accepted = service.submitBatch("B1", rows);
        assertEquals(2, accepted, "合格行应照常进账");
        assertEquals(3, store.size(), "三行都要有归宿（进账/挂账），实落 " + store.size());
        int ok = 0, fail = 0;
        for (TMgisSubmitRow r : store) {
            if (r.getStatus() == 1) {
                ok++;
            } else if (r.getStatus() == 2) {
                fail++;
            }
        }
        assertEquals(2, ok);
        assertEquals(1, fail);
    }

    /** 报回来的失败行号要对齐原表：按原表次序连排，不是坏行内部自排 */
    @Test
    void 失败行号对齐原表不不错位() {
        List<TMgisSubmitRow> rows = new ArrayList<TMgisSubmitRow>();
        rows.add(row("B1", "A1", "10"));
        rows.add(row("B1", "A2", "-1"));   // 原表第 2 行坏；-1 也算坏（不得为负）
        rows.add(row("B1", "A3", "5"));
        service.submitBatch("B1", rows);
        TMgisSubmitRow bad = null;
        for (TMgisSubmitRow r : store) {
            if ("A2".equals(r.getItemCode())) {
                bad = r;
            }
        }
        assertNotNull(bad);
        assertEquals(Integer.valueOf(2), bad.getRowNo(), "失败行号应对得回原表第 2 行");
    }

    /** 一次提交里重样的明细（同图幅代号）并一条，不许条条进账 */
    @Test
    void 重样明细并一条不重复进账() {
        List<TMgisSubmitRow> rows = new ArrayList<TMgisSubmitRow>();
        rows.add(row("B1", "DUP", "10"));
        rows.add(row("B1", "DUP", "20"));
        service.submitBatch("B1", rows);
        long entries = store.stream().filter(r -> r.getStatus() != null && r.getStatus() == 1).count();
        assertEquals(1, entries, "同代号重样明细只该进一条");
    }

    /** 重试同一批：只认头一遍，第二遍一笔不重落（幂等） */
    @Test
    void 重试同一批不长出两份() {
        List<TMgisSubmitRow> rows = new ArrayList<TMgisSubmitRow>();
        rows.add(row("B1", "A1", "10"));
        service.submitBatch("B1", rows);
        int afterFirst = store.size();
        int second = service.submitBatch("B1", new ArrayList<TMgisSubmitRow>(rows));
        assertEquals(0, second, "重递不该再进账");
        assertEquals(afterFirst, store.size(), "同批重试不该长出第二份");
    }

    /** 批号以接口入参为准，不该去翻头一行的填报值（null 也落不进空批号） */
    @Test
    void 批号取接口入参与回执同源() {
        List<TMgisSubmitRow> rows = new ArrayList<TMgisSubmitRow>();
        rows.add(row(null, "A1", "10"));   // 行上没填批号，以入参 B1 为准
        service.submitBatch("B1", rows);
        assertEquals(1, store.size());
        assertEquals("B1", store.get(0).getBatchNo());
    }
}
