package com.fc.v2.catalog;

import com.fc.v2.common.catalog.CatalogAcceptException;
import com.fc.v2.common.catalog.CatalogBookJudge;
import com.fc.v2.common.catalog.CatalogVerdict;
import com.fc.v2.model.custom.CatalogAcceptRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 行次归置复现测试：回执上的行次必须指得回原表那一行——
 * 未填的按原表次序补空号，补出来的行次序列与原表先后一致；
 * 填报行次与原表次序相悖的，行次对不上原表，整册不收。
 */
class CatalogRowNoAssignTest {

    private static CatalogAcceptRequest.Line line(Integer rowNo, String code) {
        CatalogAcceptRequest.Line l = new CatalogAcceptRequest.Line();
        l.setRowNo(rowNo);
        l.setSheetCode(code);
        l.setProdType("地形图");
        l.setRegion("临川市");
        l.setSheetCount("1");
        return l;
    }

    private static CatalogVerdict judge(CatalogAcceptRequest.Line... lines) {
        CatalogAcceptRequest req = new CatalogAcceptRequest();
        req.setBookNo("ML-ROW");
        req.setSubmitRegion("临川市");
        req.setLines(new ArrayList<CatalogAcceptRequest.Line>(Arrays.asList(lines)));
        return CatalogBookJudge.judge(req);
    }

    private static List<Integer> rowNos(CatalogVerdict v) {
        List<Integer> nos = new ArrayList<Integer>();
        for (com.fc.v2.common.catalog.CatalogLineOutcome l : v.getLines()) {
            nos.add(l.getRowNo());
        }
        return nos;
    }

    /** 填报行次占了 3，后头未填的顺次后补：3、4、5——不许倒填 1、2 错位 */
    @Test
    void 填报号在后未填行顺次后补() {
        CatalogVerdict v = judge(line(3, "A3"), line(null, "A4"), line(null, "A5"));
        assertEquals(Arrays.asList(3, 4, 5), rowNos(v));
    }

    /** 未填行夹在填报号前后：补号仍与原表次序一致 */
    @Test
    void 未填行穿插补号与原表次序一致() {
        CatalogVerdict v = judge(line(null, "A1"), line(3, "A3"), line(null, "A4"));
        assertEquals(Arrays.asList(1, 3, 4), rowNos(v));
    }

    /** 填报号占 1 而前头还有未填行：前头无处可补，行次对不上原表，整册不收 */
    @Test
    void 填报号在前未填行无处可补整册不收() {
        assertThrows(CatalogAcceptException.class,
                () -> judge(line(null, "A1"), line(1, "A2"), line(null, "A3")));
    }

    /** 填报行次与原表先后相悖：对不上原表，整册不收 */
    @Test
    void 填报行次与原表次序相悖整册不收() {
        assertThrows(CatalogAcceptException.class,
                () -> judge(line(2, "A2"), line(1, "A1")));
    }

    /** 全未填照旧自一连排（原有口径不退步） */
    @Test
    void 全未填自一连排() {
        CatalogVerdict v = judge(line(null, "A1"), line(null, "A2"), line(null, "A3"));
        assertEquals(Arrays.asList(1, 2, 3), rowNos(v));
    }
}
