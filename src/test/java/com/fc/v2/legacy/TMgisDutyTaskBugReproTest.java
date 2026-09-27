package com.fc.v2.legacy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMgisDutyTaskMapper;
import com.fc.v2.model.auto.TMgisDutyTask;
import com.fc.v2.service.impl.TMgisDutyTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 到期提醒零点派发——老通道（TMgisDutyTaskServiceImpl）毛病复现。
 *
 * <p>正确口径：没有到期件时平稳收场不抛错；派发一笔就把条目标成「已提醒」
 * 并落库（回执数与流水同出一处）；已提醒、已删除的条目不许每晚重复派发；
 * 残缺条目（到期时刻/提前日数缺失）不连坐整批。当前老实现 runOnce 空集合
 * 取 get(0) 直接越界、只数不落账、listDue 不过滤状态与删除标记。
 * 以下用例在当前实现下全部应红。</p>
 */
class TMgisDutyTaskBugReproTest {

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
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private TMgisDutyTask seed(String id, String dueAt, String amount, Integer status, Integer delFlag) {
        TMgisDutyTask t = new TMgisDutyTask();
        t.setId(Long.valueOf(id));
        t.setItemNo("D-" + id);
        t.setDueAt(at(dueAt));
        t.setAmount(amount == null ? null : new BigDecimal(amount));
        t.setStatus(status);
        t.setDelFlag(delFlag);
        store.add(t);
        return t;
    }

    /** 没有到期件：平稳收场返回 0，不许 get(0) 越界抛错 */
    @Test
    void 没有到期件平稳收场不抛错() {
        assertDoesNotThrow(() -> assertEquals(0, service.runOnce(at("2026-09-27 00:00:00"))));
    }

    /** 派发一笔要在流水上落账：条目标成已提醒(status=1)并 update，回执数=落账数 */
    @Test
    void 派发一笔就标已提醒并落账() {
        seed("1", "2026-09-26 10:00:00", "3", 0, 0);
        int sent = service.runOnce(at("2026-09-27 00:00:00"));
        assertEquals(1, sent);
        assertEquals(Integer.valueOf(1), store.get(0).getStatus(), "派发后应标成已提醒");
        verify(mapper).updateById(any(TMgisDutyTask.class));
    }

    /** 已提醒的条目第二晚不许重复派发（幂等） */
    @Test
    void 已提醒条目不重复派发() {
        seed("1", "2026-09-26 10:00:00", "3", 1, 0); // 已是「已提醒」
        List<TMgisDutyTask> due = service.listDue(at("2026-09-27 00:00:00"));
        assertTrue(due.isEmpty(), "已提醒条目不该再进到期清单");
    }

    /** 已删除条目不许被翻出来派发 */
    @Test
    void 已删除条目不进到期清单() {
        seed("1", "2026-09-26 10:00:00", "3", 0, 1);
        assertTrue(service.listDue(at("2026-09-27 00:00:00")).isEmpty());
    }

    /** 残缺条目（提前日数缺失）不连累整批：合格条目照派 */
    @Test
    void 残缺条目不连坐合格条目() {
        seed("1", "2026-09-26 10:00:00", null, 0, 0);      // amount 缺失
        seed("2", "2026-09-26 11:00:00", "2", 0, 0);        // 合格
        int sent = assertDoesNotThrow(() -> service.runOnce(at("2026-09-27 00:00:00")));
        assertEquals(1, sent, "合格条目应照常派发，残缺条目不连坐");
        verify(mapper).updateById(any(TMgisDutyTask.class));
    }

    /** 恰好到日终截止时刻即算到期（边界不丢件） */
    @Test
    void 恰好到期不丢件() {
        seed("1", "2026-09-27 00:00:00", "1", 0, 0);
        assertEquals(1, service.listDue(at("2026-09-27 00:00:00")).size(),
                "恰好在截止时刻到期的件应纳入，不能用严格小于把它漏掉");
        // 防误用：未到期的不纳入
        store.clear();
        seed("2", "2026-09-28 00:00:00", "1", 0, 0);
        assertEquals(0, service.listDue(at("2026-09-27 00:00:00")).size());
    }
}
