package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TMgisScopeCard;
import com.fc.v2.service.ITMgisScopeCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 作业范围标签建档卡 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "作业范围标签建档卡")
@Controller
@RequestMapping("/MgisScopeCardController")
public class MgisScopeCardController extends BaseController {

    private final String prefix = "admin/mgisScopeCard";

    @Autowired
    private ITMgisScopeCardService mgisScopeCardService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("mgis:mgisScopeCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "作业范围标签建档卡集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("mgis:mgisScopeCard:list")
    @ResponseBody
    public ResultTable list(TMgisScopeCard record) {
        QueryWrapper<TMgisScopeCard> queryWrapper = new QueryWrapper<TMgisScopeCard>();
        startPage();
        com.github.pagehelper.PageInfo<TMgisScopeCard> page =
                new com.github.pagehelper.PageInfo<TMgisScopeCard>(mgisScopeCardService.selectTMgisScopeCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "作业范围标签建档卡新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("mgis:mgisScopeCard:add")
    @ResponseBody
    public AjaxResult add(TMgisScopeCard record) {
        return toAjax(mgisScopeCardService.insertTMgisScopeCard(record));
    }

    @Log(title = "作业范围标签建档卡修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("mgis:mgisScopeCard:edit")
    @ResponseBody
    public AjaxResult editSave(TMgisScopeCard record) {
        return toAjax(mgisScopeCardService.updateTMgisScopeCard(record));
    }

    @Log(title = "作业范围标签建档卡删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("mgis:mgisScopeCard:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(mgisScopeCardService.deleteTMgisScopeCardByIds(ids));
    }
}
