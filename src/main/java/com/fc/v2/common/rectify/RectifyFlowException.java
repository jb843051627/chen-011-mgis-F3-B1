package com.fc.v2.common.rectify;

/**
 * 走站顶回：隔站落笔、封存后挂条目、整改站动告知三样、
 * 复测缺人硬闯之类，一律当场顶回，一笔都不写进库里。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class RectifyFlowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RectifyFlowException(String message) {
        super(message);
    }
}
