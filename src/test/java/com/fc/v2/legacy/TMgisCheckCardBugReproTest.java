package com.fc.v2.legacy;

import com.fc.v2.mapper.auto.TMgisCheckCardMapper;
import com.fc.v2.model.auto.TMgisCheckCard;
import com.fc.v2.service.impl.TMgisCheckCardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 整改回执单走站——老通道（TMgisCheckCardServiceImpl）毛病复现。
 *
 * <p>正确口径以 doc/mgis-rectify-line.md 为准（同摊业务的新门
 * POST /mgisRectify/undertake 已照此实现）：单据挪状态只能一站一站挪、
 * 封存后不再收动作、超时回滚两处状态一致、只增不物理删、
 * 同一点第二回幂等、并发抢先要顶回。以下用例在当前老实现下全部应红。</p>
 *
 * <p>库表口径（doc/schema/mgis.sql）：stage 当前关口 0..5；
 * status 0在办 1已销号 2已封存。</p>
 */
class TMgisCheckCardBugReproTest {

    private TMgisCheckCardMapper mapper;
    private TMgisCheckCardServiceImpl service;
    private final List<TMgisCheckCard> store = new ArrayList<TMgisCheckCard>();
    private long idGen = 1L;

    @BeforeEach
    void setUp() {
        mapper = mock(TMgisCheckCardMapper.class);
        service = new TMgisCheckCardServiceImpl();
        ReflectionTestUtils.setField(service, "mgisCheckCardMapper", mapper);
        when(mapper.selectById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            for (TMgisCheckCard c : store) {
                if (c.getId().equals(id)) {
                    return c;
                }
            }
            return null;
        });
        when(mapper.updateById(any())).thenAnswer(inv -> 1);
        when(mapper.deleteById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            store.removeIf(c -> c.getId().equals(id));
            return 1;
        });
    }

    private TMgisCheckCard seed(int stage, Integer status) {
        TMgisCheckCard c = new TMgisCheckCard();
        c.setId(idGen++);
        c.setBizNo("ZG-" + c.getId());
        c.setStage(stage);
        c.setStatus(status);
        store.add(c);
        return c;
    }

    /** 单据挪状态一次只许挪一站：0 → 1，不许一次跳两站 */
    @Test
    void 推进只许一站一站挪() {
        TMgisCheckCard c = seed(0, 0);
        TMgisCheckCard after = service.advance(c.getId(), "过告知");
        assertEquals(1, after.getStage(), "一次只能挪一站");
    }

    /** 到了末站（封存，stage=5，status=2）不许再推进 */
    @Test
    void 封存后不再收推进动作() {
        TMgisCheckCard c = seed(5, 2);
        TMgisCheckCard after = service.advance(c.getId(), "再推");
        assertEquals(5, after.getStage(), "封存后站次不许再变");
        assertEquals(Integer.valueOf(2), after.getStatus(), "封存状态不许被改回在办/销号");
    }

    /** 回退也是一站：从整改退回告知，stage 2 → 1，不许一刀切回 0 */
    @Test
    void 回退只许退一站不一刀切回挂号() {
        TMgisCheckCard c = seed(2, 0);
        TMgisCheckCard after = service.rollback(c.getId(), "退回补告知");
        assertEquals(1, after.getStage(), "回退只该退一站");
    }

    /**
     * 超时回滚两处状态写法要一致：更新落不了库（影响 0 行，模拟超时/被抢先），
     * 内存里的单子不许被改了状态——回话与库必须同一口径。
     */
    @Test
    void 更新落空时内存状态不与库分写() {
        TMgisCheckCard c = seed(0, 0);
        when(mapper.updateById(any())).thenReturn(0);   // 超时回滚 / 被人抢先
        service.advance(c.getId(), "过告知");
        // 库里这一笔没生效，回话对象的状态也必须停在原样
        assertEquals(0, c.getStage(), "更新落空，内存站次不该前移");
        assertEquals(Integer.valueOf(0), c.getStatus(), "更新落空，内存状态不该改成已销号");
    }

    /** 回执单只立不物理删：remove 不该真把单子从账上抹掉 */
    @Test
    void 回执单只增不改不许物理删除() {
        TMgisCheckCard c = seed(1, 0);
        service.remove(c.getId());
        assertNotNull(mapper.selectById(c.getId()), "回执单应保留在账（逻辑留痕），不该物理删除");
    }

    /** 单子不在账上：推进/回退应明确顶回，而不是悄没声回 null 当成功 */
    @Test
    void 单子不在账上明确顶回不当成功() {
        assertNull(service.advance(999L, "x")); // 现状：静默 null
        // 正确口径：controller toAjax(null!=null?1:0) 会回“操作失败”，但服务层应抛异常，
        // 此处仅钉死“不得返回一张看似推进过的假单”，并要求有显式异常更妥：
        assertThrows(RuntimeException.class, () -> service.advance(998L, "x"));
    }

    /** 同一点第二回来只认头一笔：不允许把状态又改一遍（幂等） */
    @Test
    void 同一动作重复推进不连挪两站() {
        TMgisCheckCard c = seed(0, 0);
        service.advance(c.getId(), "过告知");
        int stageAfterFirst = c.getStage();
        service.advance(c.getId(), "过告知"); // 同一点重发
        assertEquals(stageAfterFirst, c.getStage(), "同一动作重发不该再挪一站");
    }

    /** 列表/详情同一口径：推进后详情读到的 stage 与列表同源（这里钉末站常量与库表 0..5 一致） */
    @Test
    void 关口上限与库表六站口径一致() {
        // schema：stage 0..5 共六站。逐站推进六次内必须能到达封存
        TMgisCheckCard c = seed(0, 0);
        for (int i = 0; i < 5; i++) {
            TMgisCheckCard cur = service.advance(c.getId(), "挪一站");
            assertTrue(cur.getStage() <= 5, "站次不得越过库表上限 5");
        }
        assertEquals(5, c.getStage(), "逐站应能走到封存（5）");
        assertFalse(c.getStatus() == 1 && c.getStage() == 5, "到封存应是 status=2，不是销号");
    }
}
