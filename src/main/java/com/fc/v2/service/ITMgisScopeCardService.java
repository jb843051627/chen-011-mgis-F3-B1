package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TMgisScopeCard;

import java.util.List;

/**
 * 作业范围标签建档卡 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITMgisScopeCardService {

    /** 按主键查询 */
    TMgisScopeCard selectTMgisScopeCardById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TMgisScopeCard> selectTMgisScopeCardList(Wrapper<TMgisScopeCard> queryWrapper);

    /** 新增 */
    int insertTMgisScopeCard(TMgisScopeCard record);

    /** 修改 */
    int updateTMgisScopeCard(TMgisScopeCard record);

    /** 批量删除 */
    int deleteTMgisScopeCardByIds(String ids);

    /** 按主键删除 */
    int deleteTMgisScopeCardById(Long id);
}
