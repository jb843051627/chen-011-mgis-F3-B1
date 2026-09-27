package com.fc.v2.legacy;

import com.fc.v2.mapper.auto.TMgisUseBillMapper;
import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.service.impl.TMgisUseBillServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 成果提供使用逐级核准——老通道（TMgisUseBillServiceImpl）毛病复现。
 *
 * <p>几道门联动的账要拴在一处：同层「两名点齐」（sign_mode=1）时必须两人都签
 * 才挪层，已签人数（sign_count）要随落笔增长，打回（status=2）落定后不许再签挪。
 * 当前老实现 approve 不看 sign_mode/need_count、不记 sign_count，reject 不改状态。
 * 以下用例在当前实现下全部应红。</p>
 *
 * <p>库表口径（doc/schema/mgis.sql）：node_no 当前层 0..3；
 * sign_mode 0任一人 1两名点齐；status 0在核 1已核讫 2已打回。</p>
 */
class TMgisUseBillBugReproTest {

    private TMgisUseBillMapper mapper;
    private TMgisUseBillServiceImpl service;
    private final List<TMgisUseBill> store = new ArrayList<TMgisUseBill>();

    @BeforeEach
    void setUp() {
        mapper = mock(TMgisUseBillMapper.class);
        service = new TMgisUseBillServiceImpl();
        ReflectionTestUtils.setField(service, "mgisUseBillMapper", mapper);
        when(mapper.selectById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            for (TMgisUseBill b : store) {
                if (b.getId().equals(id)) {
                    return b;
                }
            }
            return null;
        });
        when(mapper.updateById(any())).thenReturn(1);
    }

    private TMgisUseBill seed(int node, int signMode, int needCount, int signCount, int status) {
        TMgisUseBill b = new TMgisUseBill();
        b.setId(1L);
        b.setBillNo("HZ-1");
        b.setNodeNo(node);
        b.setSignMode(signMode);
        b.setNeedCount(needCount);
        b.setSignCount(signCount);
        b.setStatus(status);
        store.add(b);
        return b;
    }

    /** 两名点齐模式：头一名签完不挪层，只记已签一人 */
    @Test
    void 两名点齐头一名签完不挪层() {
        TMgisUseBill b = seed(0, 1, 2, 0, 0);
        TMgisUseBill after = service.approve(1L, "甲", "同意");
        assertEquals(0, after.getNodeNo(), "两人会签，头一名签完不该挪层");
        assertEquals(Integer.valueOf(1), after.getSignCount(), "已签人数应记成 1");
        assertEquals(Integer.valueOf(0), after.getStatus(), "没点齐仍在核");
    }

    /** 两名点齐模式：第二名签完才挪到下一层 */
    @Test
    void 两名点齐第二名签完才挪层() {
        TMgisUseBill b = seed(0, 1, 2, 1, 0);
        TMgisUseBill after = service.approve(1L, "乙", "同意");
        assertEquals(1, after.getNodeNo(), "两人点齐才挪一层");
        assertEquals(Integer.valueOf(2), after.getSignCount(), "已签人数应记成 2");
    }

    /** 任一人模式：一人签即挪层，但已签人数也要落账（流水与回执同源） */
    @Test
    void 任一人模式一人即过层且已签人数落账() {
        TMgisUseBill b = seed(0, 0, 1, 0, 0);
        TMgisUseBill after = service.approve(1L, "甲", "同意");
        assertEquals(1, after.getNodeNo());
        assertEquals(Integer.valueOf(1), after.getSignCount(), "签了就该在流水上记一笔，不能层挪了人数还空着");
    }

    /** 打回必须落定：status 置 2，不悄悄一笔带过 */
    @Test
    void 打回状态要落定() {
        TMgisUseBill b = seed(1, 0, 1, 1, 0);
        TMgisUseBill after = service.reject(1L, "甲", "材料不合");
        assertEquals(Integer.valueOf(2), after.getStatus(), "打回应置 status=2（已打回）");
    }

    /** 打回定案后，两道门再来签都不许挪层（联动拴一处） */
    @Test
    void 打回定案后不许再签挪层() {
        TMgisUseBill b = seed(1, 0, 1, 1, 2);
        TMgisUseBill after = service.approve(1L, "乙", "再同意");
        assertEquals(1, after.getNodeNo(), "已打回的单不许再往前挪");
        assertEquals(Integer.valueOf(2), after.getStatus(), "状态须停在已打回");
    }

    /** 核到末层（node 3）签讫置 status=1，不许把层号顶出 0..3 */
    @Test
    void 末层签讫置核讫不越层() {
        TMgisUseBill b = seed(3, 0, 1, 0, 0);
        TMgisUseBill after = service.approve(1L, "甲", "同意");
        assertEquals(3, after.getNodeNo(), "已在末层，不许把层号顶到 4");
        assertEquals(Integer.valueOf(1), after.getStatus(), "末层签讫应置已核讫");
    }

    /** 单子不在账上：签批别抛 NPE，平稳返回 null 顶回 */
    @Test
    void 单子不在账上平稳顶回() {
        assertDoesNotThrow(() -> service.approve(999L, "甲", "同意"));
        assertDoesNotThrow(() -> service.reject(999L, "甲", "不同意"));
    }
}
