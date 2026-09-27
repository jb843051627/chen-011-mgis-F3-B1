package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMgisSubmitRowMapper;
import com.fc.v2.model.auto.TMgisSubmitRow;
import com.fc.v2.service.ITMgisSubmitRowService;

/**
 * 成果目录汇交核收行 Service业务层处理（batch-process 形状：整批提交）
 *
 * <p>章程：逐行校验，合格行入账、坏行挂驳回但不连坐，行次保留原表序；
 * 返回的成功行数与库里入账行数同源同出；空批平稳返 0；越线整批不入库返 -1；
 * 同批重递只认头一遍，不重复入库。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMgisSubmitRowServiceImpl implements ITMgisSubmitRowService {

    /** 单批行数上限：越线整批顶回，一行也不入库 */
    private static final int MAX_ROWS = 500;
    /** 行进展 0待销 1已入账 2挂驳回 */
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TMgisSubmitRowMapper mgisSubmitRowMapper;

    @Override
    public TMgisSubmitRow selectTMgisSubmitRowById(Long id) {
        return this.mgisSubmitRowMapper.selectById(id);
    }

    /**
     * 整批提交。空批平稳托住；越线整批不入库；重递认头一遍；
     * 坏行只挂自己不连坐；行次保留原表序，回执照此行次回原件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitBatch(String batchNo, List<TMgisSubmitRow> rows) {
        // 空批：平稳托住，不抛错、不落库
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        String no = resolveBatchNo(batchNo, rows);
        if (no == null) {
            throw new IllegalArgumentException("批次册号未指：整批提交须有册号");
        }

        // 重递认头一遍：该批已在账，不新落一行，回的成功数以账上为准（回执与流水同源）
        Integer existed = this.mgisSubmitRowMapper.selectCount(new QueryWrapper<TMgisSubmitRow>()
                .eq("batch_no", no));
        if (existed != null && existed > 0) {
            Integer accepted = this.mgisSubmitRowMapper.selectCount(new QueryWrapper<TMgisSubmitRow>()
                    .eq("batch_no", no).eq("status", STATUS_OK));
            return accepted == null ? 0 : accepted;
        }

        // 越线：整批顶回，一行也不入库
        if (rows.size() > MAX_ROWS) {
            return -1;
        }

        int ok = 0;
        for (int i = 0; i < rows.size(); i++) {
            TMgisSubmitRow r = rows.get(i);
            r.setBatchNo(no);
            // 行次即原表序（自 1 起）：坏行回执照此行次回原件，不重排成失败序列
            r.setRowNo(Integer.valueOf(i + 1));
            r.setDelFlag(Integer.valueOf(0));
            // 一处填错只挂这一行：代号空、幅数空或非正都挂，合格行照常入账
            if (isBadRow(r)) {
                r.setStatus(STATUS_FAIL);
            } else {
                r.setStatus(STATUS_OK);
                ok++;
            }
            try {
                this.mgisSubmitRowMapper.insert(r);
            } catch (DuplicateKeyException e) {
                // 并发同递一批：唯一键 (batch_no,row_no) 兜底，认头一遍
                Integer accepted = this.mgisSubmitRowMapper.selectCount(new QueryWrapper<TMgisSubmitRow>()
                        .eq("batch_no", no).eq("status", STATUS_OK));
                return accepted == null ? 0 : accepted;
            }
        }
        return ok;
    }

    @Override
    public List<TMgisSubmitRow> listErrors(String batchNo) {
        return this.mgisSubmitRowMapper.selectList(new QueryWrapper<TMgisSubmitRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL)
                .orderByAsc("row_no"));
    }

    /** 坏珠：代号空、幅数空或非正——只挂自己，不拖累全串 */
    private static boolean isBadRow(TMgisSubmitRow r) {
        return r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                || r.getQty() == null
                || r.getQty().compareTo(BigDecimal.ZERO) <= 0;
    }

    /** 册号先认入参，入参空再认行内带的，都没有即驳回 */
    private static String resolveBatchNo(String batchNo, List<TMgisSubmitRow> rows) {
        if (batchNo != null && !batchNo.trim().isEmpty()) {
            return batchNo.trim();
        }
        for (TMgisSubmitRow r : rows) {
            if (r.getBatchNo() != null && !r.getBatchNo().trim().isEmpty()) {
                return r.getBatchNo().trim();
            }
        }
        return null;
    }
}
