package com.czspig.productcritic.common;

public enum ReviewMode {
    MENTOR("温和导师"),
    SHARP_PM("犀利 PM"),
    CLIENT("甲方视角");

    private final String label;

    ReviewMode(String label) {
        this.label = label;
    }

    public static ReviewMode requireValid(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "mode 不能为空");
        }
        try {
            return ReviewMode.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "mode 仅支持 MENTOR、SHARP_PM、CLIENT");
        }
    }

    public String getLabel() {
        return label;
    }
}
