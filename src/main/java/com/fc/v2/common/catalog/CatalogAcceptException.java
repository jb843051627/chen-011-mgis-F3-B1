package com.fc.v2.common.catalog;

/**
 * 整册顶回：行数超过上限、一张白纸（零行）、册号或属地抬头缺失。
 * 顶回即一笔不落——白表不许回一句「已核讫」，市里据此以为交完了，
 * 年底两头账各说各话。
 *
 * <p>一行之内填错（负数幅数之类）不抛这个：那一行自己挂账，整册照走。</p>
 *
 * @author fuce
 * @date 2026-09-26
 */
public class CatalogAcceptException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CatalogAcceptException(String message) {
        super(message);
    }
}
