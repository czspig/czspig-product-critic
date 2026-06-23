package com.czspig.productcritic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateReviewRequest {

    @NotBlank(message = "产品想法不能为空")
    @Size(min = 10, max = 5000, message = "产品想法长度应为 10-5000 个字符")
    private String content;

    @NotBlank(message = "评审模式不能为空")
    private String mode;

    @NotNull(message = "吐槽强度不能为空")
    @Min(value = 1, message = "吐槽强度最小为 1")
    @Max(value = 3, message = "吐槽强度最大为 3")
    private Integer roastLevel;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getRoastLevel() {
        return roastLevel;
    }

    public void setRoastLevel(Integer roastLevel) {
        this.roastLevel = roastLevel;
    }
}
