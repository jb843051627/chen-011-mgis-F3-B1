package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMgisCheckCardMapper;
import com.fc.v2.model.auto.TMgisCheckCard;
import com.fc.v2.service.ITMgisCheckCardService;

/**
 * 不合格成果整改回执单 Service业务层处理（state-machine 形状：单据流转）
 *
 * <p>章程：推进一档就是一档，回退一档就是一档，关口与落定拴一处挪——
 * 走到末关落「已销号」，销了号的回退时关口与落定一起回；已办结（已销号、
 * 已封存）的单不再推进、内容不得再改、单不得删。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMgisCheckCardServiceImpl implements ITMgisCheckCardService {

    /** 末关：关口 0..5，走到 5 即办结 */
    private static final int MAX_STAGE = 5;
    /** 回执落定 0在办 1已销号 2已封存 */
    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_CLOSED = 1;
    private static final int STATUS_SEALED = 2;

    @javax.annotation.Resource
    private TMgisCheckCardMapper mgisCheckCardMapper;

    @Override
    public TMgisCheckCard selectTMgisCheckCardById(Long id) {
        return this.mgisCheckCardMapper.selectById(id);
    }

    @Override
    public List<TMgisCheckCard> selectTMgisCheckCardList(QueryWrapper<TMgisCheckCard> queryWrapper) {
        return this.mgisCheckCardMapper.selectList(queryWrapper);
    }

    /**
     * 推进一档：一档就是一档，不许多跨；走到末关落「已销号」；
     * 已办结的单不再推进。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMgisCheckCard advance(Long id, String remark) {
        TMgisCheckCard r = requireActive(id);
        if (r == null) {
            return null;
        }
        int stage = (r.getStage() == null ? 0 : r.getStage()) + 1;
        r.setStage(stage);
        if (stage >= MAX_STAGE) {
            r.setStatus(STATUS_CLOSED);
        }
        r.setLastAction(remark);
        this.mgisCheckCardMapper.updateById(r);
        return r;
    }

    /**
     * 回退一档：一档就是一档，不许一脚踹回起点；关口与落定拴一处——
     * 已销号的回退，落定跟着回「在办」。第 0 关无路可退被拒。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMgisCheckCard rollback(Long id, String remark) {
        if (id == null) {
            return null;
        }
        TMgisCheckCard r = this.mgisCheckCardMapper.selectById(id);
        if (r == null || r.getStatus() == null || r.getStatus() == STATUS_SEALED) {
            return null;
        }
        int stage = r.getStage() == null ? 0 : r.getStage();
        if (stage <= 0) {
            return null;
        }
        r.setStage(stage - 1);
        // 关口与落定拴一处：往回退即在办，不许留着「已销号」的旧写法
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.mgisCheckCardMapper.updateById(r);
        return r;
    }

    /** 已办结（已销号、已封存）不得再改 */
    @Override
    public boolean updateContent(Long id, String remark) {
        TMgisCheckCard r = id == null ? null : this.mgisCheckCardMapper.selectById(id);
        if (r == null || r.getStatus() == null || r.getStatus() != STATUS_ACTIVE) {
            return false;
        }
        r.setContent(remark);
        return this.mgisCheckCardMapper.updateById(r) > 0;
    }

    /** 已办结（已销号、已封存）不得删除 */
    @Override
    public boolean remove(Long id) {
        TMgisCheckCard r = id == null ? null : this.mgisCheckCardMapper.selectById(id);
        if (r == null || r.getStatus() == null || r.getStatus() != STATUS_ACTIVE) {
            return false;
        }
        return this.mgisCheckCardMapper.deleteById(id) > 0;
    }

    /** 取在办的单：单不在、已销号、已封存，一律拒（返 null） */
    private TMgisCheckCard requireActive(Long id) {
        if (id == null) {
            return null;
        }
        TMgisCheckCard r = this.mgisCheckCardMapper.selectById(id);
        if (r == null || r.getStatus() == null || r.getStatus() != STATUS_ACTIVE) {
            return null;
        }
        return r;
    }
}
