package com.fc.v2.rectify;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.rectify.RectifyAction;
import com.fc.v2.common.rectify.RectifyStation;
import com.fc.v2.mapper.auto.TMgisQualConclusionMapper;
import com.fc.v2.mapper.auto.TMgisRectifyEntryMapper;
import com.fc.v2.mapper.auto.TMgisRectifyItemMapper;
import com.fc.v2.mapper.auto.TMgisRectifyOrderMapper;
import com.fc.v2.model.auto.TMgisQualConclusion;
import com.fc.v2.model.auto.TMgisRectifyEntry;
import com.fc.v2.model.auto.TMgisRectifyItem;
import com.fc.v2.model.auto.TMgisRectifyOrder;
import com.fc.v2.model.custom.RectifyUndertakeRequest;
import com.fc.v2.model.custom.RectifyUndertakeResult;
import com.fc.v2.service.impl.TMgisRectifyOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 退回重办幂等复现测试：退回那一笔落笔后单子已挪站加轮，
 * 同一请求重试（超时重投）须只认头一笔——告知、整改、复测各站
 * 重试都认头一笔，退回这一站不许另搞一套。
 */
class RectifySendBackRetryTest {

    private TMgisRectifyOrderServiceImpl service;
    private List<TMgisRectifyOrder> orders;
    private Map<Long, List<TMgisRectifyEntry>> chains;
    private long idGen = 1000L;

    @BeforeEach
    void setUp() {
        TMgisRectifyOrderMapper orderMapper = mock(TMgisRectifyOrderMapper.class);
        TMgisRectifyEntryMapper entryMapper = mock(TMgisRectifyEntryMapper.class);
        TMgisRectifyItemMapper itemMapper = mock(TMgisRectifyItemMapper.class);
        TMgisQualConclusionMapper conclusionMapper = mock(TMgisQualConclusionMapper.class);
        service = new TMgisRectifyOrderServiceImpl();
        ReflectionTestUtils.setField(service, "mgisRectifyOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "mgisRectifyEntryMapper", entryMapper);
        ReflectionTestUtils.setField(service, "mgisRectifyItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "mgisQualConclusionMapper", conclusionMapper);

        orders = new ArrayList<TMgisRectifyOrder>();
        chains = new HashMap<Long, List<TMgisRectifyEntry>>();

        when(orderMapper.selectById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            for (TMgisRectifyOrder o : orders) {
                if (o.getId().equals(id)) {
                    return o;
                }
            }
            return null;
        });
        when(orderMapper.insert(any())).thenAnswer(inv -> {
            orders.add(inv.getArgument(0));
            return 1;
        });
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderMapper.selectCount(any())).thenReturn(0);
        when(entryMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object orderId = qw.getParamNameValuePairs().values().iterator().next();
            List<TMgisRectifyEntry> chain = chains.get(((Number) orderId).longValue());
            return chain == null ? new ArrayList<TMgisRectifyEntry>() : new ArrayList<TMgisRectifyEntry>(chain);
        });
        when(entryMapper.insert(any())).thenAnswer(inv -> {
            TMgisRectifyEntry e = inv.getArgument(0);
            e.setId(++idGen);
            chains.computeIfAbsent(e.getOrderId(), k -> new ArrayList<TMgisRectifyEntry>()).add(e);
            return 1;
        });
        when(itemMapper.insert(any())).thenAnswer(inv -> {
            ((TMgisRectifyItem) inv.getArgument(0)).setId(++idGen);
            return 1;
        });
        TMgisQualConclusion c = new TMgisQualConclusion();
        c.setId(1L);
        c.setBatchNo("B1");
        c.setItemCode("图幅A");
        c.setTier(3);
        c.setIssuedAt(day("2026-09-25"));
        when(conclusionMapper.selectList(any())).thenReturn(
                new ArrayList<TMgisQualConclusion>(Collections.singletonList(c)));
    }

    private static Date day(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static RectifyUndertakeRequest req(Long orderId, RectifyAction action) {
        RectifyUndertakeRequest r = new RectifyUndertakeRequest();
        r.setOrderId(orderId);
        r.setAction(action.name());
        return r;
    }

    /** 退回重办那一笔重投：单子已挪站加轮，仍须认头一笔，不得顶回、不得再落 */
    @Test
    void 退回重办超时重投只认头一笔() {
        Long id = service.raiseDailyOrders(day("2026-09-25")).get(0).getId();
        RectifyUndertakeRequest notify = req(id, RectifyAction.NOTIFY);
        notify.setMaps("图幅A");
        notify.setSampling("按批抽百分之十");
        notify.setEvidence("复测报告原件");
        service.undertake(notify, "张三");
        RectifyUndertakeRequest rectify = req(id, RectifyAction.RECTIFY);
        rectify.setNote("已整改");
        service.undertake(rectify, "李四");
        RectifyUndertakeRequest retest = req(id, RectifyAction.RETEST_AGENCY);
        retest.setOpinion("复测看出合不上");
        service.undertake(retest, "王五");

        RectifyUndertakeRequest back = req(id, RectifyAction.SEND_BACK);
        back.setReason("实抽幅数与告知口径合不上");
        RectifyUndertakeResult first = service.undertake(back, "赵六");
        assertEquals(RectifyStation.RECTIFY.name(), first.getOrder().getStation());
        assertEquals(Integer.valueOf(2), first.getOrder().getRoundNo());
        int pensAfterBack = chains.get(id).size();

        // 同一请求超时重投：须认头一笔，不得再落一笔，也不得顶回
        RectifyUndertakeResult retry = service.undertake(back, "赵六");
        assertTrue(retry.isDuplicated());
        assertEquals(first.getEntry().getId(), retry.getEntry().getId());
        assertEquals(pensAfterBack, chains.get(id).size());
        // 单子局面不被重投挪动
        assertEquals(RectifyStation.RECTIFY.name(), retry.getOrder().getStation());
        assertEquals(Integer.valueOf(2), retry.getOrder().getRoundNo());
    }
}
