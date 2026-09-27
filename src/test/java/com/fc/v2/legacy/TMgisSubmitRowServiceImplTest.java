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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 整批核收（成果目录汇交核收行）复现测试。
 * 接口契约（ITMgisSubmitRowService.submitBatch）：
 * 逐行校验、合法入库非法记失败明细并保留原始行号、返回成功行数；
 * 同批重复提交不重复入库；超上限返 -1 整批不入库；空批返 0。
 */
class TMgisSubmitRowServiceImplTest {

    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    private TMgisSubmitRowMapper mapper;
    private TMgisSubmitRowServiceImpl service;

    /** 假库 */
    private final List<TMgisSubmitRow> store = new ArrayList<TMgisSubmitRow>();
    private long idGen = 100L;

    @BeforeEach
    void setUp() {
        mapper = mock(TMgisSubmitRowMapper.class);
        service = new TMgisSubmitRowServiceImpl();
        ReflectionTestUtils.setField(service, "mgisSubmitRowMapper", mapper);

        when(mapper.insert(any())).thenAnswer(inv -> {
            TMgisSubmitRow r = inv.getArgument(0);
            r.setId(++idGen);
            store.add(r);
            return 1;
        });
        when(mapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            String batchNo = null;
            Integer status = null;
            for (Object v : qw.getParamNameValuePairs().values()) {
                if (v instanceof String) {
                    batchNo = (String) v;
                } else if (v instanceof Integer) {
                    status = (Integer) v;
                }
            }
            List<TMgisSubmitRow> hit = new ArrayList<TMgisSubmitRow>();
            for (TMgisSubmitRow r : store) {
                if (batchNo != null && !batchNo.equals(r.getBatchNo())) {
                    continue;
                }
                if (status != null && !status.equals(r.getStatus())) {
                    continue;
                }
                hit.add(r);
            }
            hit.sort((x, y) -> Integer.compare(
                    x.getRowNo() == null ? 0 : x.getRowNo(), y.getRowNo() == null ? 0 : y.getRowNo()));
            return hit;
        });
        when(mapper.selectCount(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            // 粗口径：假库里有该批的行即算在册（重递幂等测试用）
            String no = null;
            for (Object v : qw.getParamNameValuePairs().values()) {
                if (v instanceof String) {
                    no = (String) v;
                }
            }
            int n = 0;
            for (TMgisSubmitRow r : store) {
                if (no != null && no.equals(r.getBatchNo())) {
                    n++;
                }
            }
            return n;
        });
    }

    private static TMgisSubmitRow row(String code, String qty) {
        TMgisSubmitRow r = new TMgisSubmitRow();
        r.setItemCode(code);
        r.setQty(qty == null ? null : new BigDecimal(qty));
        return r;
    }

    private int countStatus(int status) {
        int n = 0;
        for (TMgisSubmitRow r : store) {
            if (r.getStatus() != null && r.getStatus() == status) {
                n++;
            }
        }
        return n;
    }

    /** 空批：须平稳托住返回 0，不得抛错、不得落库 */
    @Test
    void 空批次平稳托住返回零不落库() {
        assertEquals(0, service.submitBatch("B-EMPTY", new ArrayList<TMgisSubmitRow>()));
        assertEquals(0, service.submitBatch("B-NULL", null));
        verify(mapper, never()).insert(any());
    }

    /** 超上限：整批顶回返 -1，一行也不入库 */
    @Test
    void 行数超过单批上限整批不入库返负一() {
        List<TMgisSubmitRow> many = new ArrayList<TMgisSubmitRow>();
        for (int i = 0; i < 501; i++) {
            many.add(row("CODE-" + i, "1"));
        }
        assertEquals(-1, service.submitBatch("B-BIG", many));
        assertTrue(store.isEmpty());
        verify(mapper, never()).insert(any());
    }

    /** 一处填错只挂那一行：合格行照常入账，返回的是合格行数 */
    @Test
    void 坏行不连坐合格行照常入账() {
        List<TMgisSubmitRow> rows = new ArrayList<TMgisSubmitRow>();
        rows.add(row("A1", "10"));
        rows.add(row(null, "5"));        // 代号空：挂
        rows.add(row("A3", "0"));        // 幅数非正：挂
        rows.add(row("A4", "-2"));       // 负幅数：挂
        rows.add(row("A5", null));       // 幅数空：挂
        rows.add(row("A6", "7"));        // 合格
        int ok = service.submitBatch("B-MIX", rows);
        assertEquals(2, ok);
        assertEquals(2, countStatus(STATUS_OK));
        assertEquals(4, countStatus(STATUS_FAIL));
        assertEquals(6, store.size());
    }

    /** 失败行号须是原表行次，不得重排成失败序列 */
    @Test
    void 失败行明细保留原始行次不重排() {
        List<TMgisSubmitRow> rows = new ArrayList<TMgisSubmitRow>();
        rows.add(row("A1", "10"));
        TMgisSubmitRow bad = row(null, "5");
        rows.add(bad);
        rows.add(row("A3", "8"));
        service.submitBatch("B-ROWNO", rows);
        List<TMgisSubmitRow> errors = service.listErrors("B-ROWNO");
        assertEquals(1, errors.size());
        // 坏珠在原表第 2 行——回的行号必须是 2，不能重排成 1
        assertEquals(Integer.valueOf(2), errors.get(0).getRowNo());
    }

    /** 回执数与流水数同源：返回的成功行数 = 库里实际入账行数 */
    @Test
    void 返回成功行数与库里入账行数一致() {
        List<TMgisSubmitRow> rows = new ArrayList<TMgisSubmitRow>();
        rows.add(row("A1", "10"));
        rows.add(row("A2", "1"));
        rows.add(row("A3", null)); // 幅数空：挂驳回
        int ok = service.submitBatch("B-COUNT", rows);
        assertEquals(countStatus(STATUS_OK), ok);
        assertEquals(2, ok);
    }

    /** 重递幂等：同一批第二遍不重复入库 */
    @Test
    void 同一批重复提交不重复入库() {
        List<TMgisSubmitRow> rows = new ArrayList<TMgisSubmitRow>();
        rows.add(row("A1", "10"));
        int first = service.submitBatch("B-IDEM", rows);
        assertEquals(1, first);
        int second = service.submitBatch("B-IDEM", rows);
        // 第二遍认头一遍：不新落一行，回的成功数仍是头一遍那 1 行
        assertEquals(1, second);
        assertEquals(1, store.size());
        assertFalse(store.isEmpty());
    }
}
