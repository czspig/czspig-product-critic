package com.czspig.productcritic.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("review_records")
public class ReviewRecordEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sessionId;
    private String inputContent;
    private String inputSummary;
    private String mode;
    private Integer roastLevel;
    private String oneLineVerdict;
    private Integer beatScore;
    private Integer positioningScore;
    private String reportJson;
    private String reportMarkdown;
    private String status;
    private String errorMessage;
    private String modelName;
    private String promptVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getInputContent() {
        return inputContent;
    }

    public void setInputContent(String inputContent) {
        this.inputContent = inputContent;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
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

    public String getOneLineVerdict() {
        return oneLineVerdict;
    }

    public void setOneLineVerdict(String oneLineVerdict) {
        this.oneLineVerdict = oneLineVerdict;
    }

    public Integer getBeatScore() {
        return beatScore;
    }

    public void setBeatScore(Integer beatScore) {
        this.beatScore = beatScore;
    }

    public Integer getPositioningScore() {
        return positioningScore;
    }

    public void setPositioningScore(Integer positioningScore) {
        this.positioningScore = positioningScore;
    }

    public String getReportJson() {
        return reportJson;
    }

    public void setReportJson(String reportJson) {
        this.reportJson = reportJson;
    }

    public String getReportMarkdown() {
        return reportMarkdown;
    }

    public void setReportMarkdown(String reportMarkdown) {
        this.reportMarkdown = reportMarkdown;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
