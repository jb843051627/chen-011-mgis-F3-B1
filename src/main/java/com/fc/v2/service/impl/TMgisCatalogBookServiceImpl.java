package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fc.v2.common.catalog.CatalogAcceptException;
import com.fc.v2.common.catalog.CatalogBookJudge;
import com.fc.v2.common.catalog.CatalogDisposition;
import com.fc.v2.common.catalog.CatalogLineOutcome;
import com.fc.v2.common.catalog.CatalogTally;
import com.fc.v2.common.catalog.CatalogVerdict;
import com.fc.v2.mapper.auto.TMgisCatalogBookMapper;
import com.fc.v2.mapper.auto.TMgisCatalogLineMapper;
import com.fc.v2.model.auto.TMgisCatalogBook;
import com.fc.v2.model.auto.TMgisCatalogLine;
import com.fc.v2.model.custom.CatalogAcceptRequest;
import com.fc.v2.model.custom.CatalogAcceptResult;
import com.fc.v2.model.custom.CatalogBookView;
import com.fc.v2.service.ITMgisCatalogBookService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 年度图幅目录册 Service 业务层处理。
 *
 * <p>一行行拆解入账的活儿都压在这一层，核收章程的唯一出处在
 * {@link CatalogBookJudge}。本层对两张表只有 insert/select，
 * 没有 update/delete——单条添、单条改的门不辟，确实要改数走差异清单。</p>
 *
 * <p>同册号重递只认最早那一遍：先查在册，唯一键 {@code uk_catalog_book_no}
 * 兜底并发，后到的顶不掉先收的，挂账缘故也不再补第二句。两个市同日各递一册，
 * 册号各是各的，落账先后不影响任一册的定局。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
@Service
public class TMgisCatalogBookServiceImpl implements ITMgisCatalogBookService {

    @javax.annotation.Resource
    private TMgisCatalogBookMapper mgisCatalogBookMapper;

    @javax.annotation.Resource
    private TMgisCatalogLineMapper mgisCatalogLineMapper;

    /**
     * 整批核收。重递先认账：册号已在册的，不管第二遍递成什么样，都把头一遍
     * 定局原样回——后到的顶不掉先收的。头一遍才逐行推演，越线、白表在推演
     * 那一步即顶回，一笔不落；推演过得去才立册子、逐行落账，
     * 接口怎么回、账就怎么记，同出一份定局。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CatalogAcceptResult accept(CatalogAcceptRequest req) {
        // 重递是基层常态：先看册号在不在账，在册即认最早那一遍（不补第二句缘故）
        String claimedNo = req == null ? null : (req.getBookNo() == null ? null : req.getBookNo().trim());
        if (claimedNo != null && !claimedNo.isEmpty()) {
            TMgisCatalogBook existing = findByBookNo(claimedNo);
            if (existing != null) {
                return new CatalogAcceptResult(existing.getId(), rebuildVerdict(existing), true);
            }
        }

        // 头一遍：整册核成什么局面，但凭系统一笔笔推演为定
        CatalogVerdict verdict = CatalogBookJudge.judge(req);

        long bookId = IdWorker.getId();
        TMgisCatalogBook book = new TMgisCatalogBook();
        book.setId(bookId);
        book.setBookNo(verdict.getBookNo());
        book.setSubmitRegion(verdict.getSubmitRegion());
        CatalogTally tally = verdict.getTally();
        book.setTotalRows(tally.getTotalRows());
        book.setAcceptedRows(tally.getAcceptedRows());
        book.setPendingRows(tally.getPendingRows());
        book.setEntryCount(tally.getEntryCount());
        book.setSheetTotal(tally.getSheetTotal());
        try {
            this.mgisCatalogBookMapper.insert(book);
        } catch (DuplicateKeyException e) {
            // 并发同递一册：唯一键兜底，认最早那一遍，后到的原样回头一遍的定局
            TMgisCatalogBook first = requireByBookNo(verdict.getBookNo());
            return new CatalogAcceptResult(first.getId(), rebuildVerdict(first), true);
        }

        // 逐行落账：原表每一行落一条，进账、挂账都在这张表上
        for (CatalogLineOutcome line : verdict.getLines()) {
            this.mgisCatalogLineMapper.insert(toEntity(bookId, verdict.getBookNo(), line));
        }
        return new CatalogAcceptResult(bookId, verdict, false);
    }

    @Override
    public CatalogBookView viewByBookNo(String bookNo) {
        TMgisCatalogBook book = requireByBookNo(bookNo);
        return toView(book);
    }

    @Override
    public List<CatalogBookView> viewsByRegion(String submitRegion) {
        if (submitRegion == null || submitRegion.trim().isEmpty()) {
            throw new IllegalArgumentException("属地未指：凭属地翻账须说清是哪个市");
        }
        List<TMgisCatalogBook> books = this.mgisCatalogBookMapper.selectList(
                new QueryWrapper<TMgisCatalogBook>()
                        .eq("submit_region", submitRegion.trim())
                        .orderByAsc("id"));
        List<CatalogBookView> views = new ArrayList<CatalogBookView>(books.size());
        for (TMgisCatalogBook book : books) {
            views.add(toView(book));
        }
        return views;
    }

    // ---------- 翻账 ----------

    private CatalogBookView toView(TMgisCatalogBook book) {
        List<TMgisCatalogLine> lines = listLines(book.getId());
        // 三数不当册子上的死数回，自行目经同一处口径当场重算
        return new CatalogBookView(book, lines, CatalogBookJudge.tally(lines));
    }

    /** 库里的行目重新推演成回话定局：重递回的头一遍，与当日接口回话一字一板 */
    private CatalogVerdict rebuildVerdict(TMgisCatalogBook book) {
        List<TMgisCatalogLine> entities = listLines(book.getId());
        List<CatalogLineOutcome> outcomes = new ArrayList<CatalogLineOutcome>(entities.size());
        for (TMgisCatalogLine e : entities) {
            outcomes.add(new CatalogLineOutcome(e.getRowNo(), e.getSheetCode(), e.getProdType(), e.getRegion(),
                    e.getSheetCountRaw(), e.getSheetCount(),
                    CatalogDisposition.valueOf(e.getDisposition()),
                    e.getReasonCode(), e.getReasonText(), e.getPrimaryRowNo()));
        }
        return new CatalogVerdict(book.getBookNo(), book.getSubmitRegion(), outcomes,
                CatalogBookJudge.tally(entities));
    }

    private List<TMgisCatalogLine> listLines(Long bookId) {
        return this.mgisCatalogLineMapper.selectList(new QueryWrapper<TMgisCatalogLine>()
                .eq("book_id", bookId).orderByAsc("row_no"));
    }

    private TMgisCatalogBook findByBookNo(String bookNo) {
        return this.mgisCatalogBookMapper.selectOne(new QueryWrapper<TMgisCatalogBook>()
                .eq("book_no", bookNo));
    }

    private TMgisCatalogBook requireByBookNo(String bookNo) {
        if (bookNo == null || bookNo.trim().isEmpty()) {
            throw new IllegalArgumentException("册号未指");
        }
        TMgisCatalogBook book = findByBookNo(bookNo.trim());
        if (book == null) {
            throw new CatalogAcceptException("册号不在账上：" + bookNo.trim());
        }
        return book;
    }

    /** 推演一行 → 行目实体（接口怎么回、账就怎么记，字段一一对着落） */
    private static TMgisCatalogLine toEntity(Long bookId, String bookNo, CatalogLineOutcome line) {
        TMgisCatalogLine e = new TMgisCatalogLine();
        e.setBookId(bookId);
        e.setBookNo(bookNo);
        e.setRowNo(line.getRowNo());
        e.setSheetCode(line.getSheetCode());
        e.setProdType(line.getProdType());
        e.setRegion(line.getRegion());
        e.setSheetCountRaw(line.getSheetCountRaw());
        e.setSheetCount(line.getSheetCount());
        e.setDisposition(line.getDisposition().name());
        e.setReasonCode(line.getReasonCode());
        e.setReasonText(line.getReasonText());
        e.setPrimaryRowNo(line.getPrimaryRowNo());
        return e;
    }
}
