package com.fc.v2.legacy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMgisScopeCardMapper;
import com.fc.v2.mapper.auto.TMgisUnitDocMapper;
import com.fc.v2.model.auto.TMgisScopeCard;
import com.fc.v2.model.auto.TMgisUnitDoc;
import com.fc.v2.service.impl.TMgisScopeCardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 作业范围标签建档卡复现测试。
 * 契约：列表按调用方条件查（分页由调用方统一处理，服务层不得另起炉灶）；
 * 分档与判定线同一套规矩——达到上限按高一档折算，越过三档天花板仍归三档。
 */
class TMgisScopeCardServiceImplTest {

    private TMgisScopeCardMapper cardMapper;
    private TMgisUnitDocMapper unitDocMapper;
    private TMgisScopeCardServiceImpl service;
    private final List<TMgisScopeCard> store = new ArrayList<TMgisScopeCard>();

    @BeforeEach
    void setUp() {
        cardMapper = mock(TMgisScopeCardMapper.class);
        unitDocMapper = mock(TMgisUnitDocMapper.class);
        service = new TMgisScopeCardServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", cardMapper);
        ReflectionTestUtils.setField(service, "mgisUnitDocMapper", unitDocMapper);

        when(cardMapper.insert(any())).thenAnswer(inv -> {
            store.add(inv.getArgument(0));
            return 1;
        });
        when(cardMapper.selectCount(any())).thenReturn(0);
        // 在册档案：上限 30/120/360（与 doc/schema/mgis.sql 种子一致）
        TMgisUnitDoc doc = new TMgisUnitDoc();
        doc.setId(0L);
        doc.setSiteNo("ZZ00");
        doc.setStatus(0);
        doc.setTh1Max(new BigDecimal("30"));
        doc.setTh2Max(new BigDecimal("120"));
        doc.setTh3Max(new BigDecimal("360"));
        when(unitDocMapper.selectOne(any())).thenReturn(doc);
        when(unitDocMapper.selectById(any())).thenReturn(doc);
    }

    /** 列表须按调用方的条件查：服务层不得撇开条件、私设分页另搞一套 */
    @Test
    void 列表与详情同一口径_按调用方条件查() {
        List<TMgisScopeCard> all = new ArrayList<TMgisScopeCard>();
        TMgisScopeCard a = new TMgisScopeCard();
        a.setId(1L);
        a.setBillNo("KA-1");
        a.setStatus(1); // 已核对：详情查得到，列表不许私设状态门槛滤掉
        all.add(a);
        TMgisScopeCard b = new TMgisScopeCard();
        b.setId(2L);
        b.setBillNo("KA-2");
        b.setStatus(0);
        all.add(b);
        when(cardMapper.selectList(any())).thenAnswer(inv -> {
            // 假库只认调用方传来的 wrapper：服务层若另起 wrapper，这里拿到的就是它私设的
            QueryWrapper<TMgisScopeCard> qw = inv.getArgument(0);
            String seg = qw.getSqlSegment();
            List<TMgisScopeCard> hit = new ArrayList<TMgisScopeCard>();
            for (TMgisScopeCard c : all) {
                if (seg.contains("bill_no") && !"KA-1".equals(c.getBillNo())) {
                    continue;
                }
                hit.add(c);
            }
            return hit;
        });
        QueryWrapper<TMgisScopeCard> caller = new QueryWrapper<TMgisScopeCard>().eq("bill_no", "KA-1");
        List<TMgisScopeCard> list = service.selectTMgisScopeCardList(caller);
        // 详情页按主键查得到 KA-1，列表页按同一条件也须查得到
        assertEquals(1, list.size());
        assertEquals("KA-1", list.get(0).getBillNo());
    }

    /** 分档与判定线同规：达到上限按高一档折算（schema 种子注释） */
    @Test
    void 达到上限按高一档折算() {
        assertEquals(2, gradeOf("30"));   // 恰压一档顶 → 二档
        assertEquals(3, gradeOf("120"));  // 恰压二档顶 → 三档
        assertEquals(1, gradeOf("29"));
        assertEquals(2, gradeOf("31"));
    }

    /** 越过三档天花板仍归三档，不设四档 */
    @Test
    void 越过三档天花板仍归三档不设四档() {
        assertEquals(3, gradeOf("360"));  // 压三档顶仍三档
        assertEquals(3, gradeOf("999"));  // 越过也归三档
    }

    private int gradeOf(String qty) {
        store.clear();
        TMgisScopeCard card = new TMgisScopeCard();
        card.setBillNo("KA-G");
        card.setSiteId(0);
        card.setQty(new BigDecimal(qty));
        service.insertTMgisScopeCard(card);
        return card.getGradeLevel();
    }
}
