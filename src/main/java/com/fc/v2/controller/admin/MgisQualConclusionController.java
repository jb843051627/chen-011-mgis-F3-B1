package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.common.qual.NoQualLineException;
import com.fc.v2.common.qual.QualInputException;
import com.fc.v2.model.auto.TMgisQualConclusion;
import com.fc.v2.model.custom.QualIssueRequest;
import com.fc.v2.service.ITMgisQualConclusionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 测绘成果质检判定结论 Controller。
 * 只出不改：没有改、删端点——事后谁也不许手工补盖。
 *
 * @author fuce
 * @date 2026-09-26
 */
@Api(value = "测绘成果质检判定结论")
@Controller
@RequestMapping("/mgisQualConclusion")
public class MgisQualConclusionController extends BaseController {

    @Autowired
    private ITMgisQualConclusionService mgisQualConclusionService;

    @Log(title = "出判定结论", action = "issue")
    @ApiOperation(value = "出结论", notes = "出结论那一刻把判定线代号与启用之日快照落账；无可用线、浑数一律驳回不进账")
    @PostMapping("/issue")
    @RequiresPermissions("mgisQualConclusion:issue")
    @ResponseBody
    public AjaxResult issue(QualIssueRequest req) {
        try {
            return retobject(200, mgisQualConclusionService.issue(req));
        } catch (QualInputException | NoQualLineException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "判定结论台账", action = "list")
    @ApiOperation(value = "结论台账", notes = "处里核案台")
    @GetMapping("/list")
    @RequiresPermissions("mgisQualConclusion:list")
    @ResponseBody
    public ResultTable list(TMgisQualConclusion record) {
        QueryWrapper<TMgisQualConclusion> queryWrapper = new QueryWrapper<TMgisQualConclusion>();
        if (record.getItemCode() != null && !record.getItemCode().trim().isEmpty()) {
            queryWrapper.eq("item_code", record.getItemCode().trim());
        }
        if (record.getProdType() != null && !record.getProdType().trim().isEmpty()) {
            queryWrapper.eq("prod_type", record.getProdType().trim());
        }
        queryWrapper.orderByDesc("issued_at");
        startPage();
        com.github.pagehelper.PageInfo<TMgisQualConclusion> page =
                new com.github.pagehelper.PageInfo<TMgisQualConclusion>(
                        mgisQualConclusionService.selectTMgisQualConclusionList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "检验机构自查", action = "selfCheck")
    @ApiOperation(value = "检验机构自查", notes = "按图幅成果代号查结论，与核案台同一张表、同一口径")
    @GetMapping("/selfCheck")
    @RequiresPermissions("mgisQualConclusion:selfCheck")
    @ResponseBody
    public AjaxResult selfCheck(String itemCode) {
        try {
            return retobject(200, mgisQualConclusionService.selfCheck(itemCode));
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "结论详情", notes = "按主键查")
    @GetMapping("/{id}")
    @RequiresPermissions("mgisQualConclusion:list")
    @ResponseBody
    public AjaxResult detail(@PathVariable("id") Long id) {
        TMgisQualConclusion conclusion = mgisQualConclusionService.selectTMgisQualConclusionById(id);
        return conclusion == null ? error("结论不在账上") : retobject(200, conclusion);
    }
}
