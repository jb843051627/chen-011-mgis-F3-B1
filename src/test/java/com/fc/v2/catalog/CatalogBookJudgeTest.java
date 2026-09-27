package com.fc.v2.catalog;

import com.fc.v2.common.catalog.CatalogAcceptException;
import com.fc.v2.common.catalog.CatalogBookJudge;
import com.fc.v2.common.catalog.CatalogDisposition;
import com.fc.v2.common.catalog.CatalogLineOutcome;
import com.fc.v2.common.catalog.CatalogRejectReason;
import com.fc.v2.common.catalog.CatalogTally;
import com.fc.v2.common.catalog.CatalogVerdict;
import com.fc.v2.model.auto.TMgisCatalogLine;
import com.fc.v2.model.custom.CatalogAcceptRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 核收引擎单测：成果管理处那五问逐条对号——逐行交代、一处填错不连坐、
 * 重递认头一遍（口径无状态，定局与先后无关）、同代号并一条、
 * 越线白表整册不收；外加三数当场对。
 */
class CatalogBookJudgeTest {

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

    private static CatalogAcceptRequest book(String bookNo, String region, CatalogAcceptRequest.Line... lines) {
        CatalogAcceptRequest req = new CatalogAcceptRequest();
        req.setBookNo(bookNo);
        req.setSubmitRegion(region);
        req.setLines(new ArrayList<CatalogAcceptRequest.Line>(Arrays.asList(lines)));
        return req;
    }

    private static CatalogLineOutcome at(CatalogVerdict v, int rowNo) {
        for (CatalogLineOutcome l : v.getLines()) {
            if (l.getRowNo() == rowNo) {
                return l;
            }
        }
        throw new AssertionError("行次不在册：" + rowNo);
    }

    private static TMgisCatalogLine toEntity(CatalogLineOutcome l, long bookId) {
        TMgisCatalogLine e = new TMgisCatalogLine();
        e.setBookId(bookId);
        e.setRowNo(l.getRowNo());
        e.setSheetCode(l.getSheetCode());
        e.setProdType(l.getProdType());
        e.setRegion(l.getRegion());
        e.setSheetCountRaw(l.getSheetCountRaw());
        e.setSheetCount(l.getSheetCount());
        e.setDisposition(l.getDisposition().name());
        e.setReasonCode(l.getReasonCode());
        e.setReasonText(l.getReasonText());
        e.setPrimaryRowNo(l.getPrimaryRowNo());
        return e;
    }

    // ---------- 一问：逐行给结果 ----------

    @Test
    void 逐行带着行次代号类别属地四样给结果() {
        CatalogVerdict v = CatalogBookJudge.judge(book("ML-001", "临川市",
                line(null, "H49G001", "地形图", "临川市—城东区", "12"),
                line(5, "H49G002", "IMAGE_MAP", "临川市—城西区", "8")));
        assertEquals(2, v.getLines().size());
        // 未填行次按原表次序自一连排
        CatalogLineOutcome a = at(v, 1);
        assertEquals("H49G001", a.getSheetCode());
        assertEquals("TOPO_MAP", a.getProdType());
        assertEquals("临川市—城东区", a.getRegion());
        assertEquals(Integer.valueOf(12), a.getSheetCount());
        assertEquals("12", a.getSheetCountRaw());
        assertEquals(CatalogDisposition.ACCEPTED, a.getDisposition());
        // 中文名类别认得，填报行次照用
        CatalogLineOutcome b = at(v, 5);
        assertEquals("IMAGE_MAP", b.getProdType());
        assertEquals(Integer.valueOf(8), b.getSheetCount());
        assertTrue(v.getTally().balances());
        assertEquals(2, v.getTally().getAcceptedRows());
        assertEquals(0, v.getTally().getPendingRows());
        assertEquals(2, v.getTally().getEntryCount());
        assertEquals(20, v.getTally().getSheetTotal());
    }

    // ---------- 二问：一行负数，整册照走，那行自己挂着 ----------

    @Test
    void 一行幅数填成负数整册照走那行自己挂着() {
        CatalogVerdict v = CatalogBookJudge.judge(book("ML-002", "临川市",
                line(1, "H49G001", "地形图", "临川市—城东区", "12"),
                line(2, "H49G002", "影像图", "临川市—城西区", "-3"),
                line(3, "H49G003", "矢量要素", "临川市—城南区", "5")));
        CatalogTally t = v.getTally();
        // 挂着加入账等于表上总行数
        assertEquals(3, t.getTotalRows());
        assertEquals(2, t.getAcceptedRows());
        assertEquals(1, t.getPendingRows());
        assertTrue(t.balances());
        assertEquals(17, t.getSheetTotal());

        CatalogLineOutcome bad = at(v, 2);
        assertEquals(CatalogDisposition.PENDING, bad.getDisposition());
        // 行次、代号、缘故三样齐着写；浑数照原样带填报原值
        assertEquals(2, bad.getRowNo());
        assertEquals("H49G002", bad.getSheetCode());
        assertEquals(CatalogRejectReason.NEGATIVE_SHEET_COUNT.getCode(), bad.getReasonCode());
        assertTrue(bad.getReasonText().contains("幅数不得为负"));
        assertTrue(bad.getReasonText().contains("-3"));
        assertNull(bad.getSheetCount());
        // 其余两行照常进账，幅数不带上挂账那行
        assertEquals(CatalogDisposition.ACCEPTED, at(v, 1).getDisposition());
        assertEquals(CatalogDisposition.ACCEPTED, at(v, 3).getDisposition());
    }

    @Test
    void 一行同时犯好几条缘故齐着写全() {
        CatalogVerdict v = CatalogBookJudge.judge(book("ML-003", "临川市",
                line(1, "  ", "山海经图", "  ", "abc")));
        CatalogLineOutcome bad = at(v, 1);
        assertEquals(CatalogDisposition.PENDING, bad.getDisposition());
        // 代号、类别、属地、幅数四条缘故齐着写
        assertTrue(bad.getReasonText().contains("图幅代号未填"));
        assertTrue(bad.getReasonText().contains("成果类别不识"));
        assertTrue(bad.getReasonText().contains("属地名址未填"));
        assertTrue(bad.getReasonText().contains("幅数不是数"));
        assertTrue(bad.getReasonText().contains("abc"));
        // 头一条按核行次序取代号缺失
        assertEquals(CatalogRejectReason.MISSING_SHEET_CODE.getCode(), bad.getReasonCode());
        // 类别不识照留填报原值
        assertEquals("山海经图", bad.getProdType());
    }

    @Test
    void 幅数各种浑数都挂账零与正数进账() {
        CatalogVerdict v = CatalogBookJudge.judge(book("ML-004", "临川市",
                line(1, "A1", "地形图", "临川", ""),
                line(2, "A2", "地形图", "临川", "3.5"),
                line(3, "A3", "地形图", "临川", "十二"),
                line(4, "A4", "地形图", "临川", "0"),
                line(5, "A5", "地形图", "临川", "+7")));
        assertEquals(CatalogDisposition.PENDING, at(v, 1).getDisposition());
        assertEquals(CatalogRejectReason.SHEET_COUNT_NOT_A_NUMBER.getCode(), at(v, 1).getReasonCode());
        assertEquals(CatalogDisposition.PENDING, at(v, 2).getDisposition());
        assertEquals(CatalogDisposition.PENDING, at(v, 3).getDisposition());
        assertEquals(CatalogDisposition.ACCEPTED, at(v, 4).getDisposition());
        assertEquals(Integer.valueOf(0), at(v, 4).getSheetCount());
        assertEquals(CatalogDisposition.ACCEPTED, at(v, 5).getDisposition());
        assertEquals(Integer.valueOf(7), at(v, 5).getSheetCount());
    }

    // ---------- 三问：同册号重递，只认最早那一遍（引擎无状态：同输入同定局） ----------

    @Test
    void 同册号递两遍推演定局一模一样() {
        // 后递那遍改了幅数、加了行：引擎各自推演；服务层只放行头一遍（服务层另有测试）。
        // 这里保的是口径：同一遍内容无论何时算、以何次序与别册穿插，定局一字不差。
        CatalogAcceptRequest first = book("ML-005", "临川市",
                line(1, "H49G001", "地形图", "临川", "10"),
                line(2, "H49G002", "影像图", "临川", "-1"));
        CatalogVerdict v1 = CatalogBookJudge.judge(first);
        CatalogVerdict v2 = CatalogBookJudge.judge(book("ML-005", "临川市",
                line(1, "H49G001", "地形图", "临川", "10"),
                line(2, "H49G002", "影像图", "临川", "-1")));
        assertEquals(v1.getTally().getTotalRows(), v2.getTally().getTotalRows());
        assertEquals(v1.getTally().getPendingRows(), v2.getTally().getPendingRows());
        assertEquals(at(v1, 2).getReasonText(), at(v2, 2).getReasonText());
    }

    @Test
    void 两个市同日各递一册先后互换定局不变() {
        CatalogAcceptRequest cityA = book("ML-A", "临川市",
                line(1, "A001", "地形图", "临川市", "10"),
                line(2, "A002", "影像图", "临川市", "20"));
        CatalogAcceptRequest cityB = book("ML-B", "清溪市",
                line(1, "B001", "矢量要素", "清溪市", "5"));
        CatalogVerdict aThen = CatalogBookJudge.judge(cityA);
        CatalogVerdict bThen = CatalogBookJudge.judge(cityB);
        CatalogVerdict bAgain = CatalogBookJudge.judge(cityB);
        CatalogVerdict aAgain = CatalogBookJudge.judge(cityA);
        assertEquals(aThen.getTally().getSheetTotal(), aAgain.getTally().getSheetTotal());
        assertEquals(bThen.getTally().getSheetTotal(), bAgain.getTally().getSheetTotal());
        assertEquals(30, aThen.getTally().getSheetTotal());
        assertEquals(5, bThen.getTally().getSheetTotal());
        assertEquals("清溪市", bThen.getSubmitRegion());
    }

    // ---------- 四问：同图幅代号两行，并一条，幅数只加一次 ----------

    @Test
    void 同图幅代号占两行并成一条幅数只加一次() {
        CatalogVerdict v = CatalogBookJudge.judge(book("ML-006", "临川市",
                line(1, "H49G009", "地形图", "临川市—城东区", "12"),
                line(2, "H49G009", "地形图", "临川市—城西区", "15")));
        CatalogTally t = v.getTally();
        // 两行都进账，但只算一个条目、幅数只按头一行加一次
        assertEquals(2, t.getTotalRows());
        assertEquals(2, t.getAcceptedRows());
        assertEquals(0, t.getPendingRows());
        assertEquals(1, t.getEntryCount());
        assertEquals(12, t.getSheetTotal());
        assertTrue(t.balances());
        // 头行指自己，被并行指头行——追得到并在哪
        assertEquals(Integer.valueOf(1), at(v, 1).getPrimaryRowNo());
        assertTrue(at(v, 1).isPrimary());
        assertEquals(Integer.valueOf(1), at(v, 2).getPrimaryRowNo());
        assertFalse(at(v, 2).isPrimary());
    }

    @Test
    void 挂账行不参与并账且头一行若挂账后续同名行各自成组() {
        // 头一行代号空挂账；后两行同代号，正常并组，挂账行不掺和
        CatalogVerdict v = CatalogBookJudge.judge(book("ML-007", "临川市",
                line(1, null, "地形图", "临川", "12"),
                line(2, "H49G007", "影像图", "临川", "3"),
                line(3, "H49G007", "影像图", "临川", "9")));
        CatalogTally t = v.getTally();
        assertEquals(3, t.getTotalRows());
        assertEquals(2, t.getAcceptedRows());
        assertEquals(1, t.getPendingRows());
        assertEquals(1, t.getEntryCount());
        assertEquals(3, t.getSheetTotal());
        assertNull(at(v, 1).getPrimaryRowNo());
        assertEquals(Integer.valueOf(2), at(v, 3).getPrimaryRowNo());
    }

    // ---------- 五问：越线整册不收、白表不许回已核讫 ----------

    @Test
    void 行数超过上限整册不收一行也不落() {
        List<CatalogAcceptRequest.Line> many = new ArrayList<CatalogAcceptRequest.Line>();
        for (int i = 1; i <= CatalogBookJudge.MAX_ROWS + 1; i++) {
            many.add(line(i, "CODE" + i, "地形图", "临川市", "1"));
        }
        CatalogAcceptRequest big = book("ML-BIG", "临川市");
        big.setLines(many);
        CatalogAcceptException e = assertThrows(CatalogAcceptException.class, () -> CatalogBookJudge.judge(big));
        assertTrue(e.getMessage().contains("越线"));
        assertTrue(e.getMessage().contains(String.valueOf(CatalogBookJudge.MAX_ROWS)));
    }

    @Test
    void 零行白表不许回已核讫() {
        CatalogAcceptRequest blank = book("ML-BLANK", "临川市");
        blank.setLines(new ArrayList<CatalogAcceptRequest.Line>());
        CatalogAcceptException e = assertThrows(CatalogAcceptException.class, () -> CatalogBookJudge.judge(blank));
        assertTrue(e.getMessage().contains("白纸"));
        CatalogAcceptRequest nullLines = book("ML-NULL", "临川市");
        assertThrows(CatalogAcceptException.class, () -> CatalogBookJudge.judge(nullLines));
    }

    @Test
    void 册号属地抬头缺失整册不收() {
        CatalogAcceptRequest noNo = book(null, "临川市", line(1, "A1", "地形图", "临川", "1"));
        CatalogAcceptException e1 = assertThrows(CatalogAcceptException.class, () -> CatalogBookJudge.judge(noNo));
        assertTrue(e1.getMessage().contains("册号"));
        CatalogAcceptRequest noRegion = book("ML-X", "  ", line(1, "A1", "地形图", "临川", "1"));
        CatalogAcceptException e2 = assertThrows(CatalogAcceptException.class, () -> CatalogBookJudge.judge(noRegion));
        assertTrue(e2.getMessage().contains("属地"));
    }

    @Test
    void 填报行次册内撞号整册不收() {
        CatalogAcceptRequest dup = book("ML-DUP", "临川市",
                line(2, "A1", "地形图", "临川", "1"),
                line(2, "A2", "地形图", "临川", "1"));
        CatalogAcceptException e = assertThrows(CatalogAcceptException.class, () -> CatalogBookJudge.judge(dup));
        assertTrue(e.getMessage().contains("行次"));
    }

    @Test
    void 未填行次按序补且避开填报占用号() {
        CatalogVerdict v = CatalogBookJudge.judge(book("ML-GAP", "临川市",
                line(3, "A3", "地形图", "临川", "3"),
                line(null, "A1", "地形图", "临川", "1"),
                line(null, "A2", "地形图", "临川", "2")));
        assertEquals(3, at(v, 3).getRowNo());
        assertEquals(1, at(v, 1).getRowNo());
        assertEquals(2, at(v, 2).getRowNo());
    }

    // ---------- 翻账三数同出一处 ----------

    @Test
    void 三数自行目重算与落账定局同一口径() {
        CatalogVerdict v = CatalogBookJudge.judge(book("ML-008", "临川市",
                line(1, "A1", "地形图", "临川", "10"),
                line(2, "A1", "影像图", "临川", "99"),
                line(3, "A2", "矢量要素", "临川", "-5")));
        List<TMgisCatalogLine> entities = new ArrayList<TMgisCatalogLine>();
        for (CatalogLineOutcome l : v.getLines()) {
            entities.add(toEntity(l, 1L));
        }
        CatalogTally recomputed = CatalogBookJudge.tally(entities);
        assertEquals(v.getTally().getTotalRows(), recomputed.getTotalRows());
        assertEquals(v.getTally().getAcceptedRows(), recomputed.getAcceptedRows());
        assertEquals(v.getTally().getPendingRows(), recomputed.getPendingRows());
        assertEquals(v.getTally().getEntryCount(), recomputed.getEntryCount());
        assertEquals(v.getTally().getSheetTotal(), recomputed.getSheetTotal());
        assertTrue(recomputed.balances());

        // 库里少一笔：重算三数与落账定局当场对不上，幅数差追得到具体哪一行
        entities.remove(0);
        CatalogTally shortOne = CatalogBookJudge.tally(entities);
        assertFalse(shortOne.getTotalRows() == v.getTally().getTotalRows());
        assertFalse(shortOne.getSheetTotal() == v.getTally().getSheetTotal());
        assertEquals(v.getTally().getSheetTotal() - 10, shortOne.getSheetTotal());
    }
}
