package com.fc.v2.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fc.v2.common.rectify.RectifyAction;
import com.fc.v2.common.rectify.RectifyChainJudge;
import com.fc.v2.common.rectify.RectifyChainReport;
import com.fc.v2.common.rectify.RectifyFlow;
import com.fc.v2.common.rectify.RectifyFlowException;
import com.fc.v2.common.rectify.RectifyStation;
import com.fc.v2.mapper.auto.TMgisQualConclusionMapper;
import com.fc.v2.mapper.auto.TMgisRectifyEntryMapper;
import com.fc.v2.mapper.auto.TMgisRectifyItemMapper;
import com.fc.v2.mapper.auto.TMgisRectifyOrderMapper;
import com.fc.v2.model.auto.TMgisQualConclusion;
import com.fc.v2.model.auto.TMgisRectifyEntry;
import com.fc.v2.model.auto.TMgisRectifyItem;
import com.fc.v2.model.auto.TMgisRectifyOrder;
import com.fc.v2.model.custom.RectifyPageView;
import com.fc.v2.model.custom.RectifyUndertakeRequest;
import com.fc.v2.model.custom.RectifyUndertakeResult;
import com.fc.v2.service.ITMgisRectifyOrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 整改回执单 Service业务层处理。
 *
 * <p>一站站地挪、退回一站，章程都在这一层（口径唯一出处在
 * {@link RectifyFlow}）；经办、时辰在落笔那一下由系统写死，事后由不得人
 * 回头补录，也不许挪时辰。落笔链只增不改：本层对
 * {@code t_mgis_rectify_entry} 只有 insert，没有 update/delete。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@Service
public class TMgisRectifyOrderServiceImpl implements ITMgisRectifyOrderService {

    /** 零点归集立单那一笔的经办署名（系统落笔，非自然人） */
    public static final String SYSTEM_OPERATOR = "系统归集";

    @javax.annotation.Resource
    private TMgisRectifyOrderMapper mgisRectifyOrderMapper;

    @javax.annotation.Resource
    private TMgisRectifyEntryMapper mgisRectifyEntryMapper;

    @javax.annotation.Resource
    private TMgisRectifyItemMapper mgisRectifyItemMapper;

    @javax.annotation.Resource
    private TMgisQualConclusionMapper mgisQualConclusionMapper;

    /**
     * 承办。走站、顶回的章程见 {@link RectifyFlow}；落笔即写死经办与时辰，
     * 链上押哈希，同一点第二回只认头一笔。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RectifyUndertakeResult undertake(RectifyUndertakeRequest req, String operator) {
        if (req == null) {
            throw new IllegalArgumentException("承办请求为空");
        }
        if (req.getOrderId() == null) {
            throw new IllegalArgumentException("回执单号未指");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("经办未认");
        }
        TMgisRectifyOrder order = this.mgisRectifyOrderMapper.selectById(req.getOrderId());
        RectifyAction action = RectifyAction.require(req.getAction());
        // 封存、挂号笔，先顶回
        RectifyFlow.checkWritable(order, action);

        List<TMgisRectifyEntry> chain = listChain(order.getId());

        // 同一点第二回来的，只认头一笔落笔的时辰——哪怕单子已挪到下一站，
        // 这一笔也不重落，把头一笔原样回给它
        TMgisRectifyEntry dup = findAt(chain, action, order.getRoundNo());
        if (dup != null) {
            return new RectifyUndertakeResult(dup, order, true);
        }

        // 隔着站点后面那一站，不生效
        RectifyFlow.checkStation(order, action);
        // 这一笔要说的话：告知三样缺样、整改站夹带三样，都在这里顶回
        String payload = RectifyFlow.packPayload(action, req);

        TMgisRectifyEntry entry = new TMgisRectifyEntry();
        entry.setOrderId(order.getId());
        entry.setSeq(chain.size() + 1);
        entry.setRoundNo(order.getRoundNo());
        entry.setStation(action.getStation().name());
        entry.setAction(action.name());
        entry.setPayload(payload);
        // 经办、时辰由系统在落笔这一下自己写死，不收请求里的话
        entry.setPennedBy(operator.trim());
        entry.setPennedAt(nowSecond());
        entry.setPrevHash(chain.isEmpty() ? RectifyChainJudge.GENESIS
                : chain.get(chain.size() - 1).getHash());
        // 重办顶替：上一轮同站同动作的那一笔退出页面，链上原样留着
        entry.setSupersedesEntryId(previousRoundPen(chain, action, order.getRoundNo()));
        entry.setHash(RectifyChainJudge.hashOf(entry));
        try {
            this.mgisRectifyEntryMapper.insert(entry);
        } catch (DuplicateKeyException e) {
            // 并发撞上同一点：唯一键兜底，认头一笔
            throw new RectifyFlowException("这一站这一笔已有人落过，只认头一笔；请刷新页面再核");
        }

        moveStation(order, action, chain, entry);
        return new RectifyUndertakeResult(entry, order, false);
    }

    /**
     * 零点归集立单。前一日检出的不合格件（三档结论）按批归集，一批一单；
     * 同一批还有单子在途的，第二张立不出来（先查在途，唯一键兜底）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TMgisRectifyOrder> raiseDailyOrders(Date detectDay) {
        if (detectDay == null) {
            throw new IllegalArgumentException("检出日未指");
        }
        Date day0 = dayStart(detectDay);
        Date day1 = new Date(day0.getTime() + 24L * 3600 * 1000);
        List<TMgisQualConclusion> found = this.mgisQualConclusionMapper.selectList(
                new QueryWrapper<TMgisQualConclusion>()
                        .eq("tier", RectifyFlow.NONCONFORMING_TIER)
                        .ge("issued_at", day0)
                        .lt("issued_at", day1)
                        .isNotNull("batch_no")
                        .ne("batch_no", "")
                        .orderByAsc("id"));
        Map<String, List<TMgisQualConclusion>> byBatch = new LinkedHashMap<String, List<TMgisQualConclusion>>();
        for (TMgisQualConclusion c : found) {
            String batchNo = c.getBatchNo().trim();
            List<TMgisQualConclusion> bucket = byBatch.get(batchNo);
            if (bucket == null) {
                bucket = new ArrayList<TMgisQualConclusion>();
                byBatch.put(batchNo, bucket);
            }
            bucket.add(c);
        }
        List<TMgisRectifyOrder> raised = new ArrayList<TMgisRectifyOrder>();
        for (Map.Entry<String, List<TMgisQualConclusion>> batch : byBatch.entrySet()) {
            String batchNo = batch.getKey();
            Integer inflight = this.mgisRectifyOrderMapper.selectCount(new QueryWrapper<TMgisRectifyOrder>()
                    .eq("batch_no", batchNo).eq("in_flight", 1));
            if (inflight != null && inflight > 0) {
                // 同一批还有单子在途，第二张立不出来
                continue;
            }
            List<TMgisQualConclusion> items = batch.getValue();
            TMgisRectifyOrder order = new TMgisRectifyOrder();
            long id = IdWorker.getId();
            order.setId(id);
            order.setOrderNo("ZG" + id);
            order.setBatchNo(batchNo);
            // 挂号落笔即停在告知，等承办说定三样
            order.setStation(RectifyStation.NOTIFY.name());
            order.setRoundNo(1);
            order.setStatus(RectifyFlow.STATUS_OPEN);
            order.setInFlight(1);
            order.setDetectDate(day0);
            order.setItemCount(items.size());
            try {
                this.mgisRectifyOrderMapper.insert(order);
            } catch (DuplicateKeyException e) {
                // 批号 + 在途标记联合唯一兜底：在途第二张立不出来
                continue;
            }
            for (TMgisQualConclusion c : items) {
                TMgisRectifyItem item = new TMgisRectifyItem();
                item.setOrderId(id);
                item.setConclusionId(c.getId());
                item.setItemCode(c.getItemCode());
                this.mgisRectifyItemMapper.insert(item);
            }
            this.mgisRectifyEntryMapper.insert(registerPen(order));
            raised.add(order);
        }
        return raised;
    }

    @Override
    public TMgisRectifyOrder selectTMgisRectifyOrderById(Long id) {
        return this.mgisRectifyOrderMapper.selectById(id);
    }

    @Override
    public List<TMgisRectifyOrder> selectTMgisRectifyOrderList(QueryWrapper<TMgisRectifyOrder> queryWrapper) {
        return this.mgisRectifyOrderMapper.selectList(queryWrapper);
    }

    @Override
    public RectifyPageView pageView(Long orderId) {
        TMgisRectifyOrder order = requireOrder(orderId);
        Map<String, List<TMgisRectifyEntry>> page = RectifyFlow.pageOf(listChain(order.getId()));
        boolean sealed = order.getStatus() != null && order.getStatus() == RectifyFlow.STATUS_SEALED;
        List<RectifyPageView.StationPage> stations = new ArrayList<RectifyPageView.StationPage>();
        for (RectifyStation st : RectifyStation.values()) {
            // 单子停在哪一站，页面就只开哪一站的口子
            boolean open = !sealed && st.name().equals(order.getStation());
            List<TMgisRectifyEntry> entries = page.get(st.name());
            stations.add(new RectifyPageView.StationPage(st.name(), st.getLabel(), open,
                    entries == null ? Collections.<TMgisRectifyEntry>emptyList() : entries));
        }
        return new RectifyPageView(order, order.getStation(), sealed, stations);
    }

    @Override
    public RectifyChainReport verifyChain(Long orderId) {
        TMgisRectifyOrder order = requireOrder(orderId);
        List<TMgisRectifyEntry> chain = listChain(order.getId());
        RectifyChainJudge.Leg forward = RectifyChainJudge.verifyForward(chain);
        RectifyChainJudge.Leg backward = RectifyChainJudge.verifyBackward(chain);
        return new RectifyChainReport(chain.size(), forward, backward);
    }

    // ---------- 站内工序 ----------

    /** 落笔之后挪站：一站接一站；退回一站则轮次加一；封存即在途销记 */
    private void moveStation(TMgisRectifyOrder order, RectifyAction action,
                             List<TMgisRectifyEntry> chain, TMgisRectifyEntry entry) {
        boolean retestBothIn = false;
        if (action == RectifyAction.RETEST_AGENCY || action == RectifyAction.RETEST_CITY) {
            // 复测认两个人：刚落的这一名加上先前那一名，两名都进单才算走通
            RectifyAction other = action == RectifyAction.RETEST_AGENCY
                    ? RectifyAction.RETEST_CITY : RectifyAction.RETEST_AGENCY;
            retestBothIn = findAt(chain, other, order.getRoundNo()) != null;
        }
        RectifyStation next = RectifyFlow.stationAfter(action, retestBothIn);
        int newRound = order.getRoundNo() + (RectifyFlow.isSendBack(action) ? 1 : 0);

        UpdateWrapper<TMgisRectifyOrder> uw = new UpdateWrapper<TMgisRectifyOrder>();
        // 挪站带旧站旧轮次作比：被人抢先挪过即顶回，隔着站的动作不生效
        uw.eq("id", order.getId())
                .eq("station", order.getStation())
                .eq("round_no", order.getRoundNo())
                .set("station", next.name())
                .set("round_no", newRound);
        if (action == RectifyAction.NOTIFY) {
            // 告知三样快照进单：落笔即定，往后的站只读取不重填
            uw.set("notify_maps", JSONObject.parseObject(entry.getPayload()).getString(RectifyFlow.KEY_MAPS));
            uw.set("notify_sampling", JSONObject.parseObject(entry.getPayload()).getString(RectifyFlow.KEY_SAMPLING));
            uw.set("notify_evidence", JSONObject.parseObject(entry.getPayload()).getString(RectifyFlow.KEY_EVIDENCE));
            uw.set("notify_by", entry.getPennedBy());
            uw.set("notify_at", entry.getPennedAt());
        }
        if (action == RectifyAction.ARCHIVE) {
            uw.set("status", RectifyFlow.STATUS_SEALED);
            uw.set("in_flight", null);
        }
        int rows = this.mgisRectifyOrderMapper.update(null, uw);
        if (rows != 1) {
            throw new RectifyFlowException("单子已被挪动，请刷新后重来");
        }
        order.setStation(next.name());
        order.setRoundNo(newRound);
        if (action == RectifyAction.NOTIFY) {
            order.setNotifyMaps(JSONObject.parseObject(entry.getPayload()).getString(RectifyFlow.KEY_MAPS));
            order.setNotifySampling(JSONObject.parseObject(entry.getPayload()).getString(RectifyFlow.KEY_SAMPLING));
            order.setNotifyEvidence(JSONObject.parseObject(entry.getPayload()).getString(RectifyFlow.KEY_EVIDENCE));
            order.setNotifyBy(entry.getPennedBy());
            order.setNotifyAt(entry.getPennedAt());
        }
        if (action == RectifyAction.ARCHIVE) {
            order.setStatus(RectifyFlow.STATUS_SEALED);
            order.setInFlight(null);
        }
    }

    /** 挂号那一笔：零点归集系统落笔，经办署名「系统归集」 */
    private TMgisRectifyEntry registerPen(TMgisRectifyOrder order) {
        JSONObject payload = new JSONObject(true);
        payload.put("source", "零点归集");
        payload.put("detectDate", new SimpleDateFormat("yyyy-MM-dd").format(order.getDetectDate()));
        payload.put("batchNo", order.getBatchNo());
        payload.put("itemCount", order.getItemCount());
        TMgisRectifyEntry reg = new TMgisRectifyEntry();
        reg.setOrderId(order.getId());
        reg.setSeq(1);
        reg.setRoundNo(1);
        reg.setStation(RectifyStation.REGISTER.name());
        reg.setAction(RectifyAction.REGISTER.name());
        reg.setPayload(payload.toJSONString());
        reg.setPennedBy(SYSTEM_OPERATOR);
        reg.setPennedAt(nowSecond());
        reg.setPrevHash(RectifyChainJudge.GENESIS);
        reg.setHash(RectifyChainJudge.hashOf(reg));
        return reg;
    }

    /** 同一点（同站同动作同轮次）的那一笔，没有为 null */
    private TMgisRectifyEntry findAt(List<TMgisRectifyEntry> chain, RectifyAction action, Integer roundNo) {
        int round = roundNo == null ? 1 : roundNo;
        for (TMgisRectifyEntry e : chain) {
            if (action.name().equals(e.getAction())
                    && action.getStation().name().equals(e.getStation())
                    && e.getRoundNo() != null && e.getRoundNo() == round) {
                return e;
            }
        }
        return null;
    }

    /** 上一轮同站同动作的那一笔主键（重办顶替用），没有为 null */
    private Long previousRoundPen(List<TMgisRectifyEntry> chain, RectifyAction action, Integer roundNo) {
        if (action != RectifyAction.RECTIFY
                && action != RectifyAction.RETEST_AGENCY
                && action != RectifyAction.RETEST_CITY) {
            return null;
        }
        int round = roundNo == null ? 1 : roundNo;
        Long found = null;
        int foundRound = -1;
        for (TMgisRectifyEntry e : chain) {
            if (action.name().equals(e.getAction()) && e.getRoundNo() != null && e.getRoundNo() < round
                    && e.getRoundNo() > foundRound) {
                found = e.getId();
                foundRound = e.getRoundNo();
            }
        }
        return found;
    }

    private TMgisRectifyOrder requireOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("回执单号未指");
        }
        TMgisRectifyOrder order = this.mgisRectifyOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RectifyFlowException("单子不在账上：" + orderId);
        }
        return order;
    }

    /** 全链按落笔先后取出（顺拉倒拉都以此为本） */
    private List<TMgisRectifyEntry> listChain(Long orderId) {
        return this.mgisRectifyEntryMapper.selectList(new QueryWrapper<TMgisRectifyEntry>()
                .eq("order_id", orderId).orderByAsc("seq"));
    }

    /** 落笔时辰以秒计：库面 DATETIME 精确到秒，落笔与校验同一口径 */
    private static Date nowSecond() {
        return new Date(System.currentTimeMillis() / 1000L * 1000L);
    }

    private static Date dayStart(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
