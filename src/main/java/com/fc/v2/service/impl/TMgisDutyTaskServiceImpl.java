package com.fc.v2.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMgisDutyTaskMapper;
import com.fc.v2.model.auto.TMgisDutyTask;
import com.fc.v2.service.ITMgisDutyTaskService;

/**
 * 到期提醒派发条目 Service业务层处理（scheduling-job 形状：周期执行）
 *
 * <p>章程：可派条目三样齐——尚未处理（待提醒）、到了提醒窗口
 * （按单条提前提醒的自然日数，截止前 N 天起进窗口）、单条坏珠不连坐。
 * 跑一遍落一遍账：送出去的标「已提醒」，送不出去的标「送不出去」，
 * 回执的成功条数与库里落账条数同源；跑过的不再重复派。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMgisDutyTaskServiceImpl implements ITMgisDutyTaskService {

    /** 条目情形 0待提醒 1已提醒 2送不出去 */
    private static final int STATUS_WAIT = 0;
    private static final int STATUS_DONE = 1;
    private static final int STATUS_FAIL = 2;

    /** 一天的毫秒数：提前提醒窗口按自然日折算 */
    private static final long DAY_MILLIS = 24L * 3600 * 1000;

    @javax.annotation.Resource
    private TMgisDutyTaskMapper mgisDutyTaskMapper;

    @Override
    public TMgisDutyTask selectTMgisDutyTaskById(Long id) {
        return this.mgisDutyTaskMapper.selectById(id);
    }

    /**
     * 该时刻可处理的条目：尚未处理 + 已到提醒窗口（截止前 amount 天起）。
     * 时刻比较统一走毫秒值：JDBC 的 serverTimezone 与本机时区不对称，
     * 直接拿字符串比会把"恰好到期"这类边界整体带偏。
     */
    @Override
    public List<TMgisDutyTask> listDue(Date at) {
        List<TMgisDutyTask> all = this.mgisDutyTaskMapper.selectList(new QueryWrapper<TMgisDutyTask>());
        List<TMgisDutyTask> due = new java.util.ArrayList<TMgisDutyTask>();
        if (at == null || all == null) {
            return due;
        }
        for (TMgisDutyTask r : all) {
            // 尚未处理才派：已提醒、送不出去的不再重复派
            if (r.getStatus() == null || r.getStatus() != STATUS_WAIT) {
                continue;
            }
            if (r.getDueAt() == null || r.getAmount() == null) {
                continue;
            }
            long windowStart = r.getDueAt().getTime() - r.getAmount().longValue() * DAY_MILLIS;
            if (at.getTime() >= windowStart) {
                due.add(r);
            }
        }
        return due;
    }

    /**
     * 执行一次：空批平稳收 0；逐条落账，送出去的标已提醒；
     * 单条坏了标送不出去、跳过继续，不拖累全串。返回成功条数。
     */
    @Override
    public int runOnce(Date at) {
        List<TMgisDutyTask> due = listDue(at);
        if (due.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TMgisDutyTask r : due) {
            try {
                r.setStatus(STATUS_DONE);
                this.mgisDutyTaskMapper.updateById(r);
                ok++;
            } catch (RuntimeException e) {
                // 单条失败跳过继续：标送不出去，不拖累全串
                try {
                    r.setStatus(STATUS_FAIL);
                    this.mgisDutyTaskMapper.updateById(r);
                } catch (RuntimeException ignore) {
                    // 落账也失败的，留待下回再核
                }
            }
        }
        return ok;
    }
}
