package com.fc.v2.common.qual;

/**
 * 当日无可用判定线：适用范围里一条都挑不出来时明写此句，
 * 前头日子上的线不许顶上，已让位的老线不许顶上。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class NoQualLineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoQualLineException(String message) {
        super(message);
    }
}
