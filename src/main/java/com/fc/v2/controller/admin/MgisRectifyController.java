package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.common.rectify.RectifyFlowException;
import com.fc.v2.model.auto.TMgisRectifyOrder;
import com.fc.v2.model.custom.RectifyUndertakeRequest;
import com.fc.v2.service.ITMgisRectifyOrderService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 整改回执单 Controller。
 *
 * <p>门面只留「承办」一扇：告知、整改、复测、退回、销号、封存都打
 * {@code POST /mgisRectify/undertake} 进，一站站地挪、退回一站，章程都在
 * 服务层。承办到没到底，页面不许先落一笔；生效与否只认接口回话。</p>
 *
 * <p>起单、存单二门焊死：本控制器没有立单、改单、删单的端点——立单只由
 * 零点归集任务起，落笔只经承办门。经办自登录态取，不许请求代报；
 * 落笔时辰由服务层写死。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@Api(value = "整改回执单")
@Controller
@RequestMapping("/mgisRectify")
public class MgisRectifyController extends BaseController {

    @Autowired
    private ITMgisRectifyOrderService mgisRectifyOrderService;

    @Log(title = "整改承办", action = "undertake")
    @ApiOperation(value = "承办", notes = "唯一动作门：告知/整改/复测(检验机构、属地市局)/退回重办/销号/封存。"
            + "同一点第二回只认头一笔；隔站落笔、封存后挂条目、整改站动告知三样，一律当场顶回")
    @PostMapping("/undertake")
    @RequiresPermissions("mgisRectify:undertake")
    @ResponseBody
    public AjaxResult undertake(RectifyUndertakeRequest req) {
        try {
            // 经办自登录态取，落笔时辰由服务层写死——请求里说了不算
            return retobject(200, mgisRectifyOrderService.undertake(req, ShiroUtils.getLoginName()));
        } catch (RectifyFlowException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "整改回执单台账", action = "list")
    @ApiOperation(value = "回执单台账", notes = "按批号、站、状态查")
    @GetMapping("/list")
    @RequiresPermissions("mgisRectify:list")
    @ResponseBody
    public ResultTable list(TMgisRectifyOrder record) {
        QueryWrapper<TMgisRectifyOrder> queryWrapper = new QueryWrapper<TMgisRectifyOrder>();
        if (record != null) {
            if (record.getBatchNo() != null && !record.getBatchNo().trim().isEmpty()) {
                queryWrapper.eq("batch_no", record.getBatchNo().trim());
            }
            if (record.getStation() != null && !record.getStation().trim().isEmpty()) {
                queryWrapper.eq("station", record.getStation().trim());
            }
            if (record.getStatus() != null) {
                queryWrapper.eq("status", record.getStatus());
            }
        }
        queryWrapper.orderByDesc("id");
        startPage();
        com.github.pagehelper.PageInfo<TMgisRectifyOrder> page =
                new com.github.pagehelper.PageInfo<TMgisRectifyOrder>(
                        mgisRectifyOrderService.selectTMgisRectifyOrderList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "回执单页面", notes = "单子停在哪一站就只开哪一站的口子；每站只显最新一轮落笔")
    @GetMapping("/{id}")
    @RequiresPermissions("mgisRectify:list")
    @ResponseBody
    public AjaxResult detail(@PathVariable("id") Long id) {
        try {
            return retobject(200, mgisRectifyOrderService.pageView(id));
        } catch (RectifyFlowException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "落笔链校验", notes = "封存后顺拉一遍、倒拉一遍：少一笔、次序错一处、"
            + "事后补录，这批整改即不合格")
    @GetMapping("/{id}/chain")
    @RequiresPermissions("mgisRectify:list")
    @ResponseBody
    public AjaxResult chain(@PathVariable("id") Long id) {
        try {
            return retobject(200, mgisRectifyOrderService.verifyChain(id));
        } catch (RectifyFlowException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }
}
