package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fc.v2.mapper.auto.TMgisUseBillMapper;
import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.service.ITMgisUseBillService;

/**
 * 成果提供使用逐级核准单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * <p>章程：签批、否决、回退三条通道走同一套状态章程——层数与情形拴一处挪，
 * 终态（已核讫、已打回）的单子什么动作都不收。同层核准方式：任一人一票过层；
 * 两名点齐须攒够本层应签人数才挪层，挪层时已签人数清零起新一层。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMgisUseBillServiceImpl implements ITMgisUseBillService {

    /** 末层：层数签到这里即核讫 */
    private static final int MAX_NODE = 2;
    /** 同层核准方式 0任一人 1两名点齐 */
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    /** 报批情形 0在核 1已核讫 2已打回 */
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    @javax.annotation.Resource
    private TMgisUseBillMapper mgisUseBillMapper;

    @Override
    public TMgisUseBill selectTMgisUseBillById(Long id) {
        return this.mgisUseBillMapper.selectById(id);
    }

    /**
     * 签批一票。任一人模式一票过一层；两名点齐先攒人数、点齐才挪层。
     * 签满末层落「已核讫」；已核讫、已打回的单再签被拒。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMgisUseBill approve(Long id, String approver, String comment) {
        TMgisUseBill r = requireRunning(id, approver);
        if (r == null) {
            return null;
        }
        int signed = (r.getSignCount() == null ? 0 : r.getSignCount()) + 1;
        int need = r.getSignMode() != null && r.getSignMode() == MODE_AND
                ? (r.getNeedCount() == null ? 2 : r.getNeedCount()) : 1;
        if (signed < need) {
            // 两名点齐未攒够：只攒人数，不挪层
            r.setSignCount(signed);
        } else {
            // 点齐过层：挪一层，已签人数清零起新一层
            int node = (r.getNodeNo() == null ? 0 : r.getNodeNo()) + 1;
            r.setNodeNo(node);
            r.setSignCount(0);
            if (node >= MAX_NODE) {
                r.setStatus(STATUS_PASS);
            }
        }
        r.setRemark(comment);
        this.mgisUseBillMapper.updateById(r);
        return r;
    }

    /**
     * 否决。与签批同一套状态章程：在核的单落「已打回」；
     * 已核讫、已打回的单再否被拒。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMgisUseBill reject(Long id, String approver, String comment) {
        TMgisUseBill r = requireRunning(id, approver);
        if (r == null) {
            return null;
        }
        r.setStatus(STATUS_VETO);
        r.setRemark(comment);
        this.mgisUseBillMapper.updateById(r);
        return r;
    }

    /**
     * 退回上一环节。层数与情形拴一处：退一层，情形回「在核」，
     * 本层已签人数清零；已核讫的回退一样照此办理，不许两处各留各的。
     * 第 0 层无路可退、已打回的不回头，均被拒。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMgisUseBill rollback(Long id, String comment) {
        if (id == null) {
            return null;
        }
        TMgisUseBill r = this.mgisUseBillMapper.selectById(id);
        if (r == null || r.getStatus() == null || r.getStatus() == STATUS_VETO) {
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        if (node <= 0) {
            return null;
        }
        r.setNodeNo(node - 1);
        r.setSignCount(0);
        // 层数与情形拴一处：退回即在核，不许留着「已核讫」的旧写法
        r.setStatus(STATUS_RUNNING);
        r.setRemark(comment);
        this.mgisUseBillMapper.updateById(r);
        return r;
    }

    /** 取在核的单：单不在、经办空、已核讫、已打回，一律拒（返 null） */
    private TMgisUseBill requireRunning(Long id, String approver) {
        if (id == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        TMgisUseBill r = this.mgisUseBillMapper.selectById(id);
        if (r == null || r.getStatus() == null || r.getStatus() != STATUS_RUNNING) {
            return null;
        }
        return r;
    }
}
