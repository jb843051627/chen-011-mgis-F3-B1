package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.catalog.CatalogAcceptException;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.custom.CatalogAcceptRequest;
import com.fc.v2.service.ITMgisCatalogBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 年度图幅目录册核收 Controller。
 *
 * <p>此门专走「整批提交」：一册递进来只打 {@code POST /mgisCatalog/accept}，
 * 一行行拆解入账的活儿都压在服务层，整册核成什么局面但凭系统推演为定，
 * 接口怎么回、账就怎么记。单条添、单条改的门不辟——本控制器没有
 * add/edit/remove 端点；确实要改数走差异清单，不在核收记录里追加。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@Api(value = "年度图幅目录册核收")
@Controller
@RequestMapping("/mgisCatalog")
public class MgisCatalogController extends BaseController {

    @Autowired
    private ITMgisCatalogBookService mgisCatalogBookService;

    @Log(title = "目录册整批核收", action = "accept")
    @ApiOperation(value = "整批提交", notes = "一册逐行解析：进账、挂账逐行给结果，行次/代号/缘故齐着写；"
            + "一行填错只挂那行不连坐；同册号重递只认最早那一遍；同图幅代号并一条、幅数只加一次；"
            + "越线、白表整册不收")
    @PostMapping("/accept")
    @RequiresPermissions("mgisCatalog:accept")
    @ResponseBody
    public AjaxResult accept(@RequestBody CatalogAcceptRequest req) {
        try {
            return retobject(200, mgisCatalogBookService.accept(req));
        } catch (CatalogAcceptException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "凭册号翻账", notes = "册子抬头、行目、三数对账一次拉齐；"
            + "挂着加入账等于表上总行数，数字自行目当场重算")
    @GetMapping("/byBookNo")
    @RequiresPermissions("mgisCatalog:list")
    @ResponseBody
    public AjaxResult byBookNo(String bookNo) {
        try {
            return retobject(200, mgisCatalogBookService.viewByBookNo(bookNo));
        } catch (CatalogAcceptException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "凭属地翻账", notes = "把该市递过的册子各册的行目与三数对账都拉出来")
    @GetMapping("/byRegion")
    @RequiresPermissions("mgisCatalog:list")
    @ResponseBody
    public AjaxResult byRegion(String submitRegion) {
        try {
            return retobject(200, mgisCatalogBookService.viewsByRegion(submitRegion));
        } catch (CatalogAcceptException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }
}
