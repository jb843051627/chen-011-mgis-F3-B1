package com.fc.v2.catalog;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.catalog.CatalogAcceptException;
import com.fc.v2.common.catalog.CatalogDisposition;
import com.fc.v2.mapper.auto.TMgisCatalogBookMapper;
import com.fc.v2.mapper.auto.TMgisCatalogLineMapper;
import com.fc.v2.model.auto.TMgisCatalogBook;
import com.fc.v2.model.auto.TMgisCatalogLine;
import com.fc.v2.model.custom.CatalogAcceptRequest;
import com.fc.v2.model.custom.CatalogAcceptResult;
import com.fc.v2.model.custom.CatalogBookView;
import com.fc.v2.service.impl.TMgisCatalogBookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 整批核收工序单测：逐行落账、一行挂账不连坐、重递认头一遍、
 * 唯一键兜底并发、越线白表一笔不落、凭册号凭属地翻账三数当场对。
 */
class TMgisCatalogBookServiceImplTest {

    private TMgisCatalogBookMapper bookMapper;
    private TMgisCatalogLineMapper lineMapper;
    private TMgisCatalogBookServiceImpl service;

    /** 假库：册子、行目 */
    private List<TMgisCatalogBook> books;
    private List<TMgisCatalogLine> lines;
    private long idGen = 5000L;

    @BeforeEach
    void setUp() {
        bookMapper = mock(TMgisCatalogBookMapper.class);
        lineMapper = mock(TMgisCatalogLineMapper.class);
        service = new TMgisCatalogBookServiceImpl();
        ReflectionTestUtils.setField(service, "mgisCatalogBookMapper", bookMapper);
        ReflectionTestUtils.setField(service, "mgisCatalogLineMapper", lineMapper);

        books = new ArrayList<TMgisCatalogBook>();
        lines = new ArrayList<TMgisCatalogLine>();

        when(bookMapper.selectOne(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object bookNo = qw.getParamNameValuePairs().values().iterator().next();
            for (TMgisCatalogBook b : books) {
                if (b.getBookNo().equals(bookNo)) {
                    return b;
                }
            }
            return null;
        });
        when(bookMapper.insert(any())).thenAnswer(inv -> {
            TMgisCatalogBook b = inv.getArgument(0);
            if (b != null) {
                books.add(b);
            }
            return 1;
        });
        when(bookMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object region = qw.getParamNameValuePairs().values().iterator().next();
            List<TMgisCatalogBook> found = new ArrayList<TMgisCatalogBook>();
            for (TMgisCatalogBook b : books) {
                if (b.getSubmitRegion().equals(region)) {
                    found.add(b);
                }
            }
            return found;
        });
        when(lineMapper.insert(any())).thenAnswer(inv -> {
            TMgisCatalogLine l = inv.getArgument(0);
            l.setId(++idGen);
            lines.add(l);
            return 1;
        });
        when(lineMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object bookId = qw.getParamNameValuePairs().values().iterator().next();
            List<TMgisCatalogLine> found = new ArrayList<TMgisCatalogLine>();
            for (TMgisCatalogLine l : lines) {
                if (l.getBookId().equals(((Number) bookId).longValue())) {
                    found.add(l);
                }
            }
            found.sort((x, y) -> Integer.compare(x.getRowNo(), y.getRowNo()));
            return found;
        });
    }

    // ---------- 帮手 ----------

    private static CatalogAcceptRequest.Line line(Integer rowNo, String code, String type, String region, String count) {
        CatalogAcceptRequest.Line l = new CatalogAcceptRequest.Line();
        l.setRowNo(rowNo);
        l.setSheetCode(code);
        l.setProdType(type);
        l.setRegion(region);
        l.setSheetCount(count);
        return l;
    }

    private static CatalogAcceptRequest book(String bookNo, String region, CatalogAcceptRequest.Line... rows) {
        CatalogAcceptRequest req = new CatalogAcceptRequest();
        req.setBookNo(bookNo);
        req.setSubmitRegion(region);
        req.setLines(new ArrayList<CatalogAcceptRequest.Line>(Arrays.asList(rows)));
        return req;
    }

    private TMgisCatalogLine persisted(long bookId, int rowNo) {
        for (TMgisCatalogLine l : lines) {
            if (l.getBookId().equals(bookId) && l.getRowNo() == rowNo) {
                return l;
            }
        }
        throw new AssertionError("行未落账：book=" + bookId + " row=" + rowNo);
    }

    // ---------- 整批核收：逐行落账 ----------

    @Test
    void 一册进来逐行落账进账挂账分写三数齐() {
        CatalogAcceptResult r = service.accept(book("ML-100", "临川市",
                line(1, "H49G001", "地形图", "临川市—城东区", "12"),
                line(2, "H49G002", "影像图", "临川市—城西区", "-3"),
                line(3, "H49G001", "地形图", "临川市—城西区", "15")));
        assertFalse(r.isDuplicated());
        assertEquals(1, books.size());
        TMgisCatalogBook b = books.get(0);
        assertEquals("ML-100", b.getBookNo());
        assertEquals("临川市", b.getSubmitRegion());
        assertEquals(Integer.valueOf(3), b.getTotalRows());
        assertEquals(Integer.valueOf(2), b.getAcceptedRows());
        assertEquals(Integer.valueOf(1), b.getPendingRows());
        // 同代号并一条：条目 1（去重后 H49G001 一条），幅数只按头一行加一次
        assertEquals(Integer.valueOf(1), b.getEntryCount());
        assertEquals(Integer.valueOf(12), b.getSheetTotal());

        // 原表每行一条，挂账行缘故三样齐，幅数原值照留
        assertEquals(3, lines.size());
        TMgisCatalogLine bad = persisted(b.getId(), 2);
        assertEquals(CatalogDisposition.PENDING.name(), bad.getDisposition());
        assertEquals("H49G002", bad.getSheetCode());
        assertNotNull(bad.getReasonCode());
        assertTrue(bad.getReasonText().contains("-3"));
        assertEquals(null, bad.getSheetCount());
        assertEquals("-3", bad.getSheetCountRaw());
        // 被并行指头行行次
        assertEquals(Integer.valueOf(1), persisted(b.getId(), 3).getPrimaryRowNo());
        assertEquals(Integer.valueOf(1), persisted(b.getId(), 1).getPrimaryRowNo());
    }

    @Test
    void 接口回话与库里落账出自同一份推演() {
        CatalogAcceptResult r = service.accept(book("ML-101", "临川市",
                line(1, "A1", "地形图", "临川", "10"),
                line(2, "A2", "影像图", "临川", "-1")));
        // 回话逐行字段与库中实体一一对得上
        assertEquals(r.getVerdict().getLines().size(), lines.size());
        for (int row = 1; row <= 2; row++) {
            com.fc.v2.common.catalog.CatalogLineOutcome out = null;
            for (com.fc.v2.common.catalog.CatalogLineOutcome o : r.getVerdict().getLines()) {
                if (o.getRowNo() == row) {
                    out = o;
                }
            }
            TMgisCatalogLine e = persisted(r.getBookId(), row);
            assertEquals(out.getSheetCode(), e.getSheetCode());
            assertEquals(out.getProdType(), e.getProdType());
            assertEquals(out.getRegion(), e.getRegion());
            assertEquals(out.getSheetCountRaw(), e.getSheetCountRaw());
            assertEquals(out.getDisposition().name(), e.getDisposition());
            assertEquals(out.getReasonCode(), e.getReasonCode());
            assertEquals(out.getPrimaryRowNo(), e.getPrimaryRowNo());
        }
    }

    // ---------- 重递只认最早那一遍 ----------

    @Test
    void 同册号送来第二遍只认最早那一遍() {
        CatalogAcceptResult first = service.accept(book("ML-200", "临川市",
                line(1, "A1", "地形图", "临川", "10"),
                line(2, "A2", "影像图", "临川", "-1")));
        int linesAfterFirst = lines.size();

        // 第二遍改了数、又补了挂账缘故的新话：顶不掉先收的
        CatalogAcceptResult second = service.accept(book("ML-200", "临川市",
                line(1, "A1", "地形图", "临川", "999"),
                line(2, "A2", "影像图", "临川", "8"),
                line(3, "A3", "矢量要素", "临川", "7")));
        assertTrue(second.isDuplicated());
        assertEquals(first.getBookId(), second.getBookId());
        assertEquals(1, books.size());
        assertEquals(linesAfterFirst, lines.size());
        // 回的头一遍定局：挂账行、幅数合计照旧，第二句缘故没补
        assertEquals(2, second.getVerdict().getLines().size());
        assertEquals(Integer.valueOf(1), second.getVerdict().getTally().getPendingRows());
        assertEquals(Integer.valueOf(10), second.getVerdict().getTally().getSheetTotal());
        assertEquals("-1", persisted(first.getBookId(), 2).getSheetCountRaw());
    }

    @Test
    void 第二遍就是一张白纸也只回头一遍定局() {
        CatalogAcceptResult first = service.accept(book("ML-201", "临川市",
                line(1, "A1", "地形图", "临川", "10"),
                line(2, "A2", "影像图", "临川", "-1")));
        // 第二遍递成白表、又有一遍越线：都不许改口说整册不收，账上认头一遍
        CatalogAcceptRequest blank = book("ML-201", "临川市");
        blank.setLines(new ArrayList<CatalogAcceptRequest.Line>());
        CatalogAcceptResult second = service.accept(blank);
        assertTrue(second.isDuplicated());
        assertEquals(first.getBookId(), second.getBookId());
        assertEquals(2, second.getVerdict().getLines().size());

        List<CatalogAcceptRequest.Line> many = new ArrayList<CatalogAcceptRequest.Line>();
        for (int i = 1; i <= 2001; i++) {
            many.add(line(i, "C" + i, "地形图", "临川", "1"));
        }
        CatalogAcceptRequest big = book("ML-201", "临川市");
        big.setLines(many);
        CatalogAcceptResult third = service.accept(big);
        assertTrue(third.isDuplicated());
        assertEquals(Integer.valueOf(10), third.getVerdict().getTally().getSheetTotal());
        assertEquals(1, books.size());
    }

    @Test
    void 并发同递一册唯一键兜底认头一笔() {
        // insert 那一下另一笔已抢先落库：抛唯一键冲突，头一遍已在假库里
        // （doAnswer 打桩：when 式重打桩会先以空实参触发旧桩，污染假库）
        doAnswer(inv -> {
            TMgisCatalogBook winner = inv.getArgument(0);
            // 抢先那笔换个主键，但同册号
            TMgisCatalogBook first = new TMgisCatalogBook();
            first.setId(++idGen);
            first.setBookNo("ML-RACE");
            first.setSubmitRegion("临川市");
            first.setTotalRows(1);
            first.setAcceptedRows(1);
            first.setPendingRows(0);
            first.setEntryCount(1);
            first.setSheetTotal(10);
            books.add(first);
            TMgisCatalogLine l = new TMgisCatalogLine();
            l.setId(++idGen);
            l.setBookId(first.getId());
            l.setBookNo("ML-RACE");
            l.setRowNo(1);
            l.setSheetCode("A1");
            l.setProdType("TOPO_MAP");
            l.setRegion("临川");
            l.setSheetCountRaw("10");
            l.setSheetCount(10);
            l.setDisposition(CatalogDisposition.ACCEPTED.name());
            l.setPrimaryRowNo(1);
            lines.add(l);
            assertEquals("ML-RACE", winner.getBookNo());
            throw new DuplicateKeyException("uk_catalog_book_no");
        }).when(bookMapper).insert(any());
        CatalogAcceptResult r = service.accept(book("ML-RACE", "临川市",
                line(1, "A1", "地形图", "临川", "888")));
        assertTrue(r.isDuplicated());
        // 回的是抢先那笔的账，不是自己这遍的 888
        assertEquals(Integer.valueOf(10), r.getVerdict().getTally().getSheetTotal());
    }

    @Test
    void 两个市同日各递一册与分开两日办账一模一样() {
        CatalogAcceptRequest a = book("ML-A", "临川市", line(1, "A1", "地形图", "临川", "10"));
        CatalogAcceptRequest b = book("ML-B", "清溪市", line(1, "B1", "矢量要素", "清溪", "5"),
                line(2, "B1", "矢量要素", "清溪", "9"));
        // 同日连办
        CatalogAcceptResult ra = service.accept(a);
        CatalogAcceptResult rb = service.accept(b);
        assertEquals(Integer.valueOf(10), ra.getVerdict().getTally().getSheetTotal());
        // B 册同代号并一条，只加一次
        assertEquals(Integer.valueOf(1), rb.getVerdict().getTally().getEntryCount());
        assertEquals(Integer.valueOf(5), rb.getVerdict().getTally().getSheetTotal());
        assertEquals(2, books.size());

        // 换个空库，次序倒过来办：定局一字不差
        books.clear();
        lines.clear();
        CatalogAcceptResult rb2 = service.accept(b);
        CatalogAcceptResult ra2 = service.accept(a);
        assertEquals(ra.getVerdict().getTally().getSheetTotal(), ra2.getVerdict().getTally().getSheetTotal());
        assertEquals(rb.getVerdict().getTally().getSheetTotal(), rb2.getVerdict().getTally().getSheetTotal());
        assertEquals(rb.getVerdict().getTally().getEntryCount(), rb2.getVerdict().getTally().getEntryCount());
    }

    // ---------- 越线、白表一笔不落 ----------

    @Test
    void 越线白表整册不收一笔不落() {
        List<CatalogAcceptRequest.Line> many = new ArrayList<CatalogAcceptRequest.Line>();
        for (int i = 1; i <= 2001; i++) {
            many.add(line(i, "C" + i, "地形图", "临川", "1"));
        }
        CatalogAcceptRequest big = book("ML-BIG", "临川市");
        big.setLines(many);
        assertThrows(CatalogAcceptException.class, () -> service.accept(big));
        assertEquals(0, books.size());
        assertEquals(0, lines.size());

        CatalogAcceptRequest blank = book("ML-BLANK", "临川市");
        blank.setLines(new ArrayList<CatalogAcceptRequest.Line>());
        CatalogAcceptException e = assertThrows(CatalogAcceptException.class, () -> service.accept(blank));
        assertTrue(e.getMessage().contains("白纸"));
        assertEquals(0, books.size());
        assertEquals(0, lines.size());
    }

    // ---------- 翻账 ----------

    @Test
    void 凭册号翻账行目三数一次拉齐且当场重算() {
        CatalogAcceptResult r = service.accept(book("ML-300", "临川市",
                line(1, "A1", "地形图", "临川", "10"),
                line(2, "A1", "影像图", "临川", "20"),
                line(3, "A2", "矢量要素", "临川", "-5")));
        CatalogBookView view = service.viewByBookNo("ML-300");
        assertEquals(r.getBookId(), view.getBook().getId());
        assertEquals(3, view.getLines().size());
        // 三数自行目当场重算，与落账定局同口径：挂着加入账等于总行数
        assertEquals(3, view.getTally().getTotalRows());
        assertEquals(2, view.getTally().getAcceptedRows());
        assertEquals(1, view.getTally().getPendingRows());
        assertEquals(1, view.getTally().getEntryCount());
        assertEquals(10, view.getTally().getSheetTotal());
        assertTrue(view.getTally().balances());
    }

    @Test
    void 凭册号翻账册号不在账上顶回() {
        CatalogAcceptException e = assertThrows(CatalogAcceptException.class,
                () -> service.viewByBookNo("ML-NOPE"));
        assertTrue(e.getMessage().contains("不在账上"));
    }

    @Test
    void 凭属地翻账只出本市册子() {
        service.accept(book("ML-C1", "临川市", line(1, "A1", "地形图", "临川", "1")));
        service.accept(book("ML-C2", "清溪市", line(1, "B1", "地形图", "清溪", "2")));
        service.accept(book("ML-C3", "临川市", line(1, "A2", "地形图", "临川", "3")));
        List<CatalogBookView> linchuan = service.viewsByRegion("临川市");
        assertEquals(2, linchuan.size());
        assertEquals("ML-C1", linchuan.get(0).getBook().getBookNo());
        assertEquals("ML-C3", linchuan.get(1).getBook().getBookNo());
        // 每册三数都对得上
        for (CatalogBookView v : linchuan) {
            assertTrue(v.getTally().balances());
            assertEquals(v.getBook().getTotalRows().intValue(), v.getTally().getTotalRows());
        }
        assertEquals(1, service.viewsByRegion("清溪市").size());
        assertThrows(IllegalArgumentException.class, () -> service.viewsByRegion("  "));
    }
}
