package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.rectify.RectifyChainReport;
import com.fc.v2.model.auto.TMgisRectifyOrder;
import com.fc.v2.model.custom.RectifyPageView;
import com.fc.v2.model.custom.RectifyUndertakeRequest;
import com.fc.v2.model.custom.RectifyUndertakeResult;

import java.util.Date;
import java.util.List;

/**
 * 整改回执单 Service接口。
 *
 * <p>门面只留「承办」一扇：告知、整改、复测、退回、销号、封存都打
 * {@link #undertake} 进，一站站地挪、退回一站，章程都在服务层（走站口径的
 * 唯一出处是 {@link com.fc.v2.common.rectify.RectifyFlow}）。</p>
 *
 * <p>起单、存单二门焊死：{@link #raiseDailyOrders} 只供零点归集任务调用，
 * 控制器不开起单、存单的口子。</p>
 *
 * <p>本接口已发布的方法签名照原样封存；添新功能请新起重载，不许改旧签名。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
public interface ITMgisRectifyOrderService {

    /**
     * 承办：唯一动作门。经办与落笔时辰由系统在落笔那一下写死，
     * 不收请求里的话；同一点第二回来的，只认头一笔落笔的时辰。
     *
     * @param req      承办请求（单号、动作、这一笔要说的话）
     * @param operator 经办（控制器自登录态取，不许请求代报）
     * @return 落了的那一笔与单子眼下状态
     */
    RectifyUndertakeResult undertake(RectifyUndertakeRequest req, String operator);

    /**
     * 零点归集立单（起单门焊死，只供定时任务）：把检出日全天检出的不合格件
     * 按批归集，一批立一张；同一批只要还有单子在途，第二张便立不出来。
     *
     * @param detectDay 检出日（归集该日 00:00 至次日 00:00 检出的件）
     * @return 立起的单子（在途撞单的批不在其列）
     */
    List<TMgisRectifyOrder> raiseDailyOrders(Date detectDay);

    /**
     * 按主键查单
     */
    TMgisRectifyOrder selectTMgisRectifyOrderById(Long id);

    /**
     * 台账查询
     */
    List<TMgisRectifyOrder> selectTMgisRectifyOrderList(QueryWrapper<TMgisRectifyOrder> queryWrapper);

    /**
     * 回执单页面：只开眼下那一站的口子；每站只显最新一轮落笔的话。
     */
    RectifyPageView pageView(Long orderId);

    /**
     * 整链校验：顺拉从挂号念到封存，倒拉从封存念回挂号，
     * 两程都通这批整改才算合格。
     */
    RectifyChainReport verifyChain(Long orderId);
}
