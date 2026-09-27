package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.common.qual.NoQualLineException;
import com.fc.v2.common.qual.QualInputException;
import com.fc.v2.common.qual.QualLineJudge;
import com.fc.v2.common.qual.QualVerdict;
import com.fc.v2.model.auto.TMgisQualLine;
import com.fc.v2.service.ITMgisQualLineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 测绘成果质检判定线 Controller：线的录改撤、当日适用线查询、试算。
 * 判定口径在 QualLineJudge 一处，此处只做进出。
 *
 * @author fuce
 * @date 2026-09-26
 */
@Api(value = "测绘成果质检判定线")
@Controller
@RequestMapping("/mgisQualLine")
public class MgisQualLineController extends BaseController {

    @Autowired
    private ITMgisQualLineService mgisQualLineService;

    @Log(title = "测绘成果质检判定线台账", action = "list")
    @ApiOperation(value = "判定线台账", notes = "判定线台账")
    @GetMapping("/list")
    @RequiresPermissions("mgisQualLine:list")
    @ResponseBody
    public ResultTable list(TMgisQualLine record) {
        QueryWrapper<TMgisQualLine> queryWrapper = new QueryWrapper<TMgisQualLine>();
        queryWrapper.eq("del_flag", 0);
        if (record.getRuleCode() != null && !record.getRuleCode().trim().isEmpty()) {
            queryWrapper.like("rule_code", record.getRuleCode().trim());
        }
        if (record.getProdType() != null && !record.getProdType().trim().isEmpty()) {
            queryWrapper.eq("prod_type", record.getProdType().trim());
        }
        queryWrapper.orderByAsc("prod_type").orderByDesc("priority").orderByDesc("rule_code");
        startPage();
        com.github.pagehelper.PageInfo<TMgisQualLine> page =
                new com.github.pagehelper.PageInfo<TMgisQualLine>(
                        mgisQualLineService.selectTMgisQualLineList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "测绘成果质检判定线录入", action = "add")
    @ApiOperation(value = "录线", notes = "录线")
    @PostMapping("/add")
    @RequiresPermissions("mgisQualLine:add")
    @ResponseBody
    public AjaxResult add(TMgisQualLine line) {
        try {
            return toAjax(mgisQualLineService.insertTMgisQualLine(line));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "测绘成果质检判定线改线", action = "edit")
    @ApiOperation(value = "改线", notes = "改线（不动已落账的结论）")
    @PostMapping("/edit")
    @RequiresPermissions("mgisQualLine:edit")
    @ResponseBody
    public AjaxResult edit(TMgisQualLine line) {
        try {
            return toAjax(mgisQualLineService.updateTMgisQualLine(line));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "测绘成果质检判定线撤线", action = "remove")
    @ApiOperation(value = "撤线", notes = "撤线（逻辑删除）")
    @PostMapping("/remove")
    @RequiresPermissions("mgisQualLine:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        try {
            return toAjax(mgisQualLineService.deleteTMgisQualLineByIds(ids));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "当日适用判定线", action = "applicable")
    @ApiOperation(value = "当日适用判定线", notes = "交件日当日管得着这份成果的线，核案台与自查页面同此一口")
    @GetMapping("/applicable")
    @RequiresPermissions("mgisQualLine:list")
    @ResponseBody
    public AjaxResult applicable(String prodType, Integer scaleDenominator, String day) {
        try {
            Date submitDay = QualLineJudge.parseDay(day);
            List<TMgisQualLine> hit = mgisQualLineService.listApplicable(prodType, scaleDenominator, submitDay);
            if (hit.isEmpty()) {
                return error("当日无可用判定线");
            }
            return retobject(200, hit);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "判定线试算", action = "judge")
    @ApiOperation(value = "试算", notes = "判定全流程试算，不落账")
    @PostMapping("/judge")
    @RequiresPermissions("mgisQualLine:judge")
    @ResponseBody
    public AjaxResult judge(String prodType, Integer scaleDenominator, String submitDay, String meanError) {
        try {
            QualVerdict verdict = mgisQualLineService.judge(
                    prodType, scaleDenominator, QualLineJudge.parseDay(submitDay), meanError);
            return retobject(200, verdict);
        } catch (QualInputException | NoQualLineException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }
}
