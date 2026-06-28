package com.czspig.productcritic.dto;

public class ReviewDetailResponse {

    private Long id;
    private String inputContent;
    private String inputSummary;
    private String mode;
    private Integer roastLevel;
    private String oneLineVerdict;
    private Integer beatScore;
    private Integer positioningScore;
    private ReviewReportDto report;
    private String reportJson;
    private String reportMarkdown;
    private String status;
    private String errorMessage;
    private String providerName;
    private String modelName;
    private Boolean fallbackUsed;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public ReviewReportDto getReport() {
        return report;
    }

    public void setReport(ReviewReportDto report) {
        this.report = report;
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

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(Boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
