package com.czspig.productcritic.dto;

import java.util.ArrayList;
import java.util.List;

public class ProjectWorkbenchResponse {

    private String projectId;
    private String title;
    private Long latestReviewId;
    private Integer latestVersionNo;
    private String updatedAt;
    private Integer totalVersions;
    private String decision;
    private String decisionReason;
    private String oneLineVerdict;
    private Integer beatScore;
    private Integer positioningScore;
    private Integer overallScore;
    private String qualityStatus;
    private String qualitySummary;
    private List<ReviewReportDto.ProvidedFact> providedFacts = new ArrayList<>();
    private List<ReviewReportDto.Inference> inferences = new ArrayList<>();
    private List<ReviewReportDto.CriticalHypothesis> criticalHypotheses = new ArrayList<>();
    private List<ReviewReportDto.UnknownQuestion> unknownQuestions = new ArrayList<>();
    private String successMetric;
    private List<String> validationPlan = new ArrayList<>();
    private List<ReviewGroupResponse.ReviewVersionItem> versions = new ArrayList<>();

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getLatestReviewId() {
        return latestReviewId;
    }

    public void setLatestReviewId(Long latestReviewId) {
        this.latestReviewId = latestReviewId;
    }

    public Integer getLatestVersionNo() {
        return latestVersionNo;
    }

    public void setLatestVersionNo(Integer latestVersionNo) {
        this.latestVersionNo = latestVersionNo;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getTotalVersions() {
        return totalVersions;
    }

    public void setTotalVersions(Integer totalVersions) {
        this.totalVersions = totalVersions;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
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

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }

    public String getQualitySummary() {
        return qualitySummary;
    }

    public void setQualitySummary(String qualitySummary) {
        this.qualitySummary = qualitySummary;
    }

    public List<ReviewReportDto.ProvidedFact> getProvidedFacts() {
        return providedFacts;
    }

    public void setProvidedFacts(List<ReviewReportDto.ProvidedFact> providedFacts) {
        this.providedFacts = providedFacts;
    }

    public List<ReviewReportDto.Inference> getInferences() {
        return inferences;
    }

    public void setInferences(List<ReviewReportDto.Inference> inferences) {
        this.inferences = inferences;
    }

    public List<ReviewReportDto.CriticalHypothesis> getCriticalHypotheses() {
        return criticalHypotheses;
    }

    public void setCriticalHypotheses(List<ReviewReportDto.CriticalHypothesis> criticalHypotheses) {
        this.criticalHypotheses = criticalHypotheses;
    }

    public List<ReviewReportDto.UnknownQuestion> getUnknownQuestions() {
        return unknownQuestions;
    }

    public void setUnknownQuestions(List<ReviewReportDto.UnknownQuestion> unknownQuestions) {
        this.unknownQuestions = unknownQuestions;
    }

    public String getSuccessMetric() {
        return successMetric;
    }

    public void setSuccessMetric(String successMetric) {
        this.successMetric = successMetric;
    }

    public List<String> getValidationPlan() {
        return validationPlan;
    }

    public void setValidationPlan(List<String> validationPlan) {
        this.validationPlan = validationPlan;
    }

    public List<ReviewGroupResponse.ReviewVersionItem> getVersions() {
        return versions;
    }

    public void setVersions(List<ReviewGroupResponse.ReviewVersionItem> versions) {
        this.versions = versions;
    }
}
