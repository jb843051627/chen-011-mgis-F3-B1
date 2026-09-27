package com.fc.v2.legacy;

import com.fc.v2.mapper.auto.TMgisDutyTaskMapper;
import com.fc.v2.model.auto.TMgisDutyTask;
import com.fc.v2.service.impl.TMgisDutyTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 到期提醒派发（scheduling-job）复现测试。
 * 接口契约：listDue 只出"执行窗口内 + 到期 + 尚未处理"的条目；
 * runOnce 返回成功条数，单条失败跳过继续，跑过的条目落"已提醒"。
 */
class TMgisDutyTaskServiceImplTest {

    private static final int STATUS_WAIT = 0;
    private static final int STATUS_DONE = 1;

    private TMgisDutyTaskMapper mapper;
    private TMgisDutyTaskServiceImpl service;
    private final List<TMgisDutyTask> store = new ArrayList<TMgisDutyTask>();

    @BeforeEach
    void setUp() {
        mapper = mock(TMgisDutyTaskMapper.class);
        service = new TMgisDutyTaskServiceImpl();
        ReflectionTestUtils.setField(service, "mgisDutyTaskMapper", mapper);
        when(mapper.selectList(any())).thenAnswer(inv -> new ArrayList<TMgisDutyTask>(store));
        when(mapper.updateById(any())).thenAnswer(inv -> 1);
    }

    private static Date at(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private TMgisDutyTask task(long id, String dueAt, String amountDays, int status) {
        TMgisDutyTask t = new TMgisDutyTask();
        t.setId(id);
        t.setItemNo("DQ-" + id);
        t.setDueAt(at(dueAt));
        t.setAmount(amountDays == null ? null : new BigDecimal(amountDays));
        t.setStatus(status);
        store.add(t);
        return t;
    }

    /** 已提醒过的不再派：同一批重跑不得长出第二份 */
    @Test
    void 已处理的条目不再重复派发() {
        task(1L, "2026-09-20 23:59:59", "3", STATUS_WAIT);
        task(2L, "2026-09-20 23:59:59", "3", STATUS_DONE);
        List<TMgisDutyTask> due = service.listDue(at("2026-09-21 00:00:00"));
        assertEquals(1, due.size());
        assertEquals(Long.valueOf(1L), due.get(0).getId());
    }

    /** 还没到提醒窗口的不派：提前 N 天才进窗口，不是过了截止才算到期 */
    @Test
    void 提前提醒窗口按单条天数算() {
        // 截止 9-25，提前 5 天：9-21 已在窗口内
        task(1L, "2026-09-25 23:59:59", "5", STATUS_WAIT);
        // 截止 9-25，提前 2 天：9-21 还没进窗口
        task(2L, "2026-09-25 23:59:59", "2", STATUS_WAIT);
        List<TMgisDutyTask> due = service.listDue(at("2026-09-21 08:00:00"));
        assertEquals(1, due.size());
        assertEquals(Long.valueOf(1L), due.get(0).getId());
    }

    /** 空批：没有到期条目时平稳收 0，不得抛错 */
    @Test
    void 没有到期条目平稳收零() {
        assertEquals(0, service.runOnce(at("2026-09-21 00:00:00")));
        task(9L, "2026-09-30 23:59:59", "1", STATUS_WAIT); // 远未到期
        assertEquals(0, service.runOnce(at("2026-09-21 00:00:00")));
    }

    /** 跑一遍要落账：成功的条目标"已提醒"，回执数与流水数同源 */
    @Test
    void 跑过的条目标已提醒回执与落账一致() {
        task(1L, "2026-09-20 23:59:59", "3", STATUS_WAIT);
        task(2L, "2026-09-20 23:59:59", "3", STATUS_WAIT);
        int ok = service.runOnce(at("2026-09-21 00:00:00"));
        assertEquals(2, ok);
        // 落账：两条都标了已提醒；再跑一遍不再重复派
        assertEquals(STATUS_DONE, store.get(0).getStatus().intValue());
        assertEquals(STATUS_DONE, store.get(1).getStatus().intValue());
        assertEquals(0, service.runOnce(at("2026-09-21 01:00:00")));
    }

    /** 一颗坏珠不拖累全串：单条坏了跳过，其余照常 */
    @Test
    void 单条失败跳过继续不连坐() {
        task(1L, "2026-09-20 23:59:59", "3", STATUS_WAIT);
        task(2L, "2026-09-20 23:59:59", null, STATUS_WAIT); // 缺提前天数的坏珠
        task(3L, "2026-09-20 23:59:59", "3", STATUS_WAIT);
        int ok = service.runOnce(at("2026-09-21 00:00:00"));
        assertEquals(2, ok);
        assertEquals(STATUS_DONE, store.get(0).getStatus().intValue());
        assertEquals(STATUS_DONE, store.get(2).getStatus().intValue());
        assertTrue(store.get(1).getStatus().intValue() != STATUS_DONE);
    }
}
