package com.czspig.productcritic.dto;

public class ReviewListItemResponse {

    private Long id;
    private String inputSummary;
    private String ideaGroupId;
    private Integer versionNo;
    private Long parentReviewId;
    private Integer groupVersionCount;
    private String mode;
    private Integer roastLevel;
    private String oneLineVerdict;
    private Integer beatScore;
    private Integer positioningScore;
    private String status;
    private String errorMessage;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getIdeaGroupId() {
        return ideaGroupId;
    }

    public void setIdeaGroupId(String ideaGroupId) {
        this.ideaGroupId = ideaGroupId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Long getParentReviewId() {
        return parentReviewId;
    }

    public void setParentReviewId(Long parentReviewId) {
        this.parentReviewId = parentReviewId;
    }

    public Integer getGroupVersionCount() {
        return groupVersionCount;
    }

    public void setGroupVersionCount(Integer groupVersionCount) {
        this.groupVersionCount = groupVersionCount;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
