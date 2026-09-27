package com.fc.v2.legacy;

import com.fc.v2.mapper.auto.TMgisCheckCardMapper;
import com.fc.v2.model.auto.TMgisCheckCard;
import com.fc.v2.service.impl.TMgisCheckCardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 整改回执单（state-machine）复现测试。
 * 实体契约：stage 当前关口 0..5；status 0在办 1已销号 2已封存。
 * 接口契约：推进一档、回退一档；已办结不得再改、不得删除。
 */
class TMgisCheckCardServiceImplTest {

    private static final int MAX_STAGE = 5;
    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_CLOSED = 1;

    private TMgisCheckCardMapper mapper;
    private TMgisCheckCardServiceImpl service;
    private final Map<Long, TMgisCheckCard> store = new HashMap<Long, TMgisCheckCard>();

    @BeforeEach
    void setUp() {
        mapper = mock(TMgisCheckCardMapper.class);
        service = new TMgisCheckCardServiceImpl();
        ReflectionTestUtils.setField(service, "mgisCheckCardMapper", mapper);
        when(mapper.selectById(any())).thenAnswer(inv -> store.get(inv.getArgument(0)));
        when(mapper.updateById(any())).thenAnswer(inv -> 1);
        when(mapper.deleteById(any())).thenAnswer(inv -> {
            store.remove(inv.getArgument(0));
            return 1;
        });
    }

    private TMgisCheckCard card(long id, int stage, int status) {
        TMgisCheckCard c = new TMgisCheckCard();
        c.setId(id);
        c.setBizNo("HZ-" + id);
        c.setStage(stage);
        c.setStatus(status);
        store.put(id, c);
        return c;
    }

    /** 推进一档就是一档，不许一步跨两档 */
    @Test
    void 推进一次只走一档() {
        card(1L, 0, STATUS_ACTIVE);
        TMgisCheckCard r = service.advance(1L, "推进");
        assertNotNull(r);
        assertEquals(1, r.getStage().intValue());
        assertEquals(STATUS_ACTIVE, r.getStatus().intValue());
    }

    /** 走到末关落"已销号"，再往前的门就关了 */
    @Test
    void 走满关口落销号后不再推进() {
        card(2L, MAX_STAGE - 1, STATUS_ACTIVE);
        TMgisCheckCard r = service.advance(2L, "最后一关");
        assertNotNull(r);
        assertEquals(MAX_STAGE, r.getStage().intValue());
        assertEquals(STATUS_CLOSED, r.getStatus().intValue());
        // 已办结：再推进被拒
        assertNull(service.advance(2L, "再推"));
        assertEquals(MAX_STAGE, store.get(2L).getStage().intValue());
    }

    /** 回退一档就是一档，不许一脚踹回起点 */
    @Test
    void 回退一次只退一档() {
        card(3L, 3, STATUS_ACTIVE);
        TMgisCheckCard r = service.rollback(3L, "退回");
        assertNotNull(r);
        assertEquals(2, r.getStage().intValue());
        assertEquals(STATUS_ACTIVE, r.getStatus().intValue());
    }

    /** 已销号的单回退：关口与落定拴一处，回在办 */
    @Test
    void 已销号回退状态一并回在办() {
        card(4L, MAX_STAGE, STATUS_CLOSED);
        TMgisCheckCard r = service.rollback(4L, "销号后发现错漏");
        assertNotNull(r);
        assertEquals(MAX_STAGE - 1, r.getStage().intValue());
        assertEquals(STATUS_ACTIVE, r.getStatus().intValue());
    }

    /** 已办结的单：内容不得再改、单不得删 */
    @Test
    void 已办结不得再改不得删() {
        card(5L, MAX_STAGE, STATUS_CLOSED);
        assertFalse(service.updateContent(5L, "事后改话"));
        assertFalse(service.remove(5L));
        assertNotNull(store.get(5L));
        // 在办的单可以改、可以删
        card(6L, 1, STATUS_ACTIVE);
        assertTrue(service.updateContent(6L, "补充整改记事"));
        assertTrue(service.remove(6L));
        assertNull(store.get(6L));
    }
}
