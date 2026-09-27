package com.fc.v2.common.qual;

/**
 * 浑数驳回：中误差填成负数、一长串九之类，系统照原样驳回，休想进账。
 * 异常里带着填报原值，回单上写的就是它，不许改写、不许四舍五入。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class QualInputException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 填报原值（照原样驳回） */
    private final String rawInput;

    public QualInputException(String message, String rawInput) {
        super(message);
        this.rawInput = rawInput;
    }

    public String getRawInput() {
        return rawInput;
    }
}
