package com.fc.v2.legacy;

import com.fc.v2.mapper.auto.TMgisUseBillMapper;
import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.service.impl.TMgisUseBillServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 逐级核准单（approval-chain）复现测试。
 * 实体契约：node_no 当前所在层；sign_mode 0任一人/1两名点齐；
 * need_count 本层应签人数；sign_count 本层已签人数；
 * status 0在核 1已核讫 2已打回。
 */
class TMgisUseBillServiceImplTest {

    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;
    private static final int MAX_NODE = 2;

    private TMgisUseBillMapper mapper;
    private TMgisUseBillServiceImpl service;
    private final Map<Long, TMgisUseBill> store = new HashMap<Long, TMgisUseBill>();

    @BeforeEach
    void setUp() {
        mapper = mock(TMgisUseBillMapper.class);
        service = new TMgisUseBillServiceImpl();
        ReflectionTestUtils.setField(service, "mgisUseBillMapper", mapper);
        when(mapper.selectById(any())).thenAnswer(inv -> store.get(inv.getArgument(0)));
        when(mapper.updateById(any())).thenAnswer(inv -> 1);
    }

    private TMgisUseBill bill(long id, int node, int signMode, int needCount, int status) {
        TMgisUseBill b = new TMgisUseBill();
        b.setId(id);
        b.setBillNo("PZH-" + id);
        b.setNodeNo(node);
        b.setSignMode(signMode);
        b.setNeedCount(needCount);
        b.setSignCount(0);
        b.setStatus(status);
        store.put(id, b);
        return b;
    }

    /** 任一人模式：一票过一层，签满层数核讫 */
    @Test
    void 任一人模式逐层签批到核讫() {
        bill(1L, 0, MODE_OR, 1, STATUS_RUNNING);
        TMgisUseBill r = service.approve(1L, "张三", "同意");
        assertNotNull(r);
        assertEquals(1, r.getNodeNo().intValue());
        assertEquals(STATUS_RUNNING, r.getStatus().intValue());
        r = service.approve(1L, "李四", "同意");
        assertEquals(MAX_NODE, r.getNodeNo().intValue());
        assertEquals(STATUS_PASS, r.getStatus().intValue());
    }

    /** 两名点齐：本层应签人数未攒够不得挪层 */
    @Test
    void 两名点齐须攒够人数才挪层() {
        bill(2L, 0, MODE_AND, 2, STATUS_RUNNING);
        TMgisUseBill r = service.approve(2L, "张三", "同意");
        assertNotNull(r);
        // 头一票只攒人数，不挪层
        assertEquals(0, r.getNodeNo().intValue());
        assertEquals(1, r.getSignCount().intValue());
        assertEquals(STATUS_RUNNING, r.getStatus().intValue());
        r = service.approve(2L, "李四", "同意");
        // 点齐才挪层，已签人数清零起新一层
        assertEquals(1, r.getNodeNo().intValue());
        assertEquals(0, r.getSignCount().intValue());
    }

    /** 已核讫的单不许再签：终态操作被拒返 null */
    @Test
    void 已核讫再签被拒() {
        bill(3L, MAX_NODE, MODE_OR, 1, STATUS_PASS);
        assertNull(service.approve(3L, "王五", "又签"));
        assertEquals(STATUS_PASS, store.get(3L).getStatus().intValue());
        assertEquals(MAX_NODE, store.get(3L).getNodeNo().intValue());
    }

    /** 否决须落定"已打回"，不许签了跟没签一样 */
    @Test
    void 否决落已打回() {
        bill(4L, 1, MODE_OR, 1, STATUS_RUNNING);
        TMgisUseBill r = service.reject(4L, "李四", "材料不齐");
        assertNotNull(r);
        assertEquals(STATUS_VETO, r.getStatus().intValue());
        // 打回后再签批、再否决都拒
        assertNull(service.approve(4L, "张三", "同意"));
        assertNull(service.reject(4L, "张三", "再否"));
    }

    /** 回退：层数与情形拴一处——退回上一层，情形回"在核" */
    @Test
    void 回退一层状态与层数同挪() {
        bill(5L, 1, MODE_OR, 1, STATUS_RUNNING);
        TMgisUseBill r = service.rollback(5L, "材料退回补正");
        assertNotNull(r);
        assertEquals(0, r.getNodeNo().intValue());
        assertEquals(STATUS_RUNNING, r.getStatus().intValue());
    }

    /** 已核讫的单回退：层数退、情形也得跟着退回"在核"，不许两处各留各的 */
    @Test
    void 已核讫回退状态一并回在核() {
        bill(6L, MAX_NODE, MODE_OR, 1, STATUS_PASS);
        TMgisUseBill r = service.rollback(6L, "核讫后发现错漏退回");
        assertNotNull(r);
        assertEquals(MAX_NODE - 1, r.getNodeNo().intValue());
        assertEquals(STATUS_RUNNING, r.getStatus().intValue());
    }

    /** 第 0 层无路可退：被拒返 null */
    @Test
    void 头一层再退被拒() {
        bill(7L, 0, MODE_OR, 1, STATUS_RUNNING);
        assertNull(service.rollback(7L, "退无可退"));
        assertEquals(0, store.get(7L).getNodeNo().intValue());
    }
}
