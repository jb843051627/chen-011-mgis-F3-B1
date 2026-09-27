package com.fc.v2.service;

import com.fc.v2.model.custom.CatalogAcceptRequest;
import com.fc.v2.model.custom.CatalogAcceptResult;
import com.fc.v2.model.custom.CatalogBookView;

import java.util.List;

/**
 * 年度图幅目录册 Service 接口。
 *
 * <p>此门专走「整批提交」：一册递进来只经 {@link #accept} 一处，一行行拆解
 * 入账的活儿都压在服务层（核收口径的唯一出处是
 * {@link com.fc.v2.common.catalog.CatalogBookJudge}）；整册核成什么局面，
 * 但凭系统一笔笔推演为定，接口怎么回、账就怎么记。</p>
 *
 * <p>单条添、单条改的门不辟：行目只立不改，确实要改数走差异清单，
 * 不在核收记录里追加自家话。已发布的方法签名一仍其旧；
 * 欲扩充另起新重载。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
public interface ITMgisCatalogBookService {

    /**
     * 整批核收。逐行解析：收下的进账，没收下的挂账（行次、代号、缘故齐着写）；
     * 一行填错只挂那行，不连坐；同图幅代号并一条、幅数只加一次；
     * 越线、白表整册不收。同册号重递只认最早那一遍，回的是头一遍的定局。
     *
     * @param req 整批提交请求
     * @return 核收回话（逐行结果、三数对账；重递时 duplicated 为真）
     */
    CatalogAcceptResult accept(CatalogAcceptRequest req);

    /**
     * 凭册号翻账：册子抬头、行目、三数对账一次拉齐。
     * 册号不在账上即顶回。
     */
    CatalogBookView viewByBookNo(String bookNo);

    /**
     * 凭属地翻账：把该市递过的册子各册的抬头、行目、三数对账都拉出来。
     *
     * @param submitRegion 递交属地（册子抬头）
     */
    List<CatalogBookView> viewsByRegion(String submitRegion);
}
