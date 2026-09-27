package com.fc.v2.common.quartz.task;

import com.fc.v2.model.auto.TMgisRectifyOrder;
import com.fc.v2.service.ITMgisRectifyOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 整改回执单零点归集任务：每天零点把前一日检出的不合格件归集进来立单。
 *
 * <p>调度配置：在定时任务台账里挂 {@code mgisRectifyTask.raiseDailyOrders()}，
 * cron 用 {@code 0 0 0 * * ?}（每日零点）。起单一门焊死——这是立单的唯一来路，
 * 控制器不起单。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@Component("mgisRectifyTask")
public class MgisRectifyTask {

    @Autowired
    private ITMgisRectifyOrderService mgisRectifyOrderService;

    /**
     * 归集前一日检出的不合格件，按批立单；同一批还有单子在途的，第二张立不出来。
     */
    public void raiseDailyOrders() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = c.getTime();
        List<TMgisRectifyOrder> raised = mgisRectifyOrderService.raiseDailyOrders(yesterday);
        System.out.println("整改回执单零点归集：立单 " + raised.size() + " 张");
    }
}
