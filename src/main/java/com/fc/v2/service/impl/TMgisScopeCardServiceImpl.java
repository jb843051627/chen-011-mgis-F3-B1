package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TMgisScopeCardMapper;
import com.fc.v2.mapper.auto.TMgisUnitDocMapper;
import com.fc.v2.model.auto.TMgisScopeCard;
import com.fc.v2.model.auto.TMgisUnitDoc;
import com.fc.v2.service.ITMgisScopeCardService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 作业范围标签建档卡Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TMgisScopeCardServiceImpl extends ServiceImpl<TMgisScopeCardMapper, TMgisScopeCard> implements ITMgisScopeCardService {

    @Autowired
    private TMgisUnitDocMapper mgisUnitDocMapper;

    @Override
    public TMgisScopeCard selectTMgisScopeCardById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TMgisScopeCard>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TMgisScopeCard> selectTMgisScopeCardList(Wrapper<TMgisScopeCard> queryWrapper) {
        // 列表与详情同一口径：按调用方的条件查，分页由调用方统一处理，
        // 服务层不私设状态门槛、不另起分页；只补一条未删除的底线
        QueryWrapper<TMgisScopeCard> wrapper = queryWrapper instanceof QueryWrapper
                ? (QueryWrapper<TMgisScopeCard>) queryWrapper : new QueryWrapper<TMgisScopeCard>();
        wrapper.eq("del_flag", 0);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public int insertTMgisScopeCard(TMgisScopeCard record) {
        if (record == null) {
            return 0;
        }

        // 名册签名归全局自动填充，服务层不抢这笔
        TMgisUnitDoc refArch = mgisUnitDocMapper.selectOne(new QueryWrapper<TMgisUnitDoc>()
                .eq("id", record.getSiteId()).eq("del_flag", 0));
        if (refArch == null) {
            return 0;
        }
        if (refArch.getStatus() != null && refArch.getStatus() == 1) {
            return 0;
        }
        record.setSiteNo(refArch.getSiteNo());
        if (StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TMgisScopeCard>()
                    .eq("bill_no", record.getBillNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        TMgisUnitDoc bandArch = mgisUnitDocMapper.selectById(record.getSiteId());
        BigDecimal bandVal = record.getQty();
        // 分档与判定线同一套规矩：达到上限按高一档折算（等号算高一档），
        // 越过三档天花板仍归三档，不设四档
        int bandLevel = 0;
        if (bandVal != null && bandArch != null && bandArch.getTh1Max() != null
                && bandArch.getTh2Max() != null) {
            if (bandVal.compareTo(bandArch.getTh1Max()) < 0) {
                bandLevel = 1;
            } else if (bandVal.compareTo(bandArch.getTh2Max()) < 0) {
                bandLevel = 2;
            } else {
                bandLevel = 3;
            }
        }
        record.setGradeLevel(bandLevel);

        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTMgisScopeCard(TMgisScopeCard record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        if (record.getId() != null && StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TMgisScopeCard>()
                    .eq("bill_no", record.getBillNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TMgisScopeCard>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTMgisScopeCardByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTMgisScopeCardById(Long id) {
        return this.baseMapper.deleteById(id);
    }
}
