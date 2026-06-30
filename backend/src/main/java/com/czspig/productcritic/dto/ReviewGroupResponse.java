package com.czspig.productcritic.dto;

import java.util.ArrayList;
import java.util.List;

public class ReviewGroupResponse {

    private String ideaGroupId;
    private List<ReviewVersionItem> versions = new ArrayList<>();

    public String getIdeaGroupId() {
        return ideaGroupId;
    }

    public void setIdeaGroupId(String ideaGroupId) {
        this.ideaGroupId = ideaGroupId;
    }

    public List<ReviewVersionItem> getVersions() {
        return versions;
    }

    public void setVersions(List<ReviewVersionItem> versions) {
        this.versions = versions;
    }

    public static class ReviewVersionItem {

        private Long id;
        private Integer versionNo;
        private Long parentReviewId;
        private String goDecision;
        private String goDecisionReason;
        private Integer beatScore;
        private Integer positioningScore;
        private String oneLineVerdict;
        private String successMetric;
        private String minimumBuildGoal;
        private List<String> coreFeatures = new ArrayList<>();
        private List<String> excludedFeatures = new ArrayList<>();
        private List<String> validationPlan = new ArrayList<>();
        private String createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public String getGoDecision() {
            return goDecision;
        }

        public void setGoDecision(String goDecision) {
            this.goDecision = goDecision;
        }

        public String getGoDecisionReason() {
            return goDecisionReason;
        }

        public void setGoDecisionReason(String goDecisionReason) {
            this.goDecisionReason = goDecisionReason;
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

        public String getOneLineVerdict() {
            return oneLineVerdict;
        }

        public void setOneLineVerdict(String oneLineVerdict) {
            this.oneLineVerdict = oneLineVerdict;
        }

        public String getSuccessMetric() {
            return successMetric;
        }

        public void setSuccessMetric(String successMetric) {
            this.successMetric = successMetric;
        }

        public String getMinimumBuildGoal() {
            return minimumBuildGoal;
        }

        public void setMinimumBuildGoal(String minimumBuildGoal) {
            this.minimumBuildGoal = minimumBuildGoal;
        }

        public List<String> getCoreFeatures() {
            return coreFeatures;
        }

        public void setCoreFeatures(List<String> coreFeatures) {
            this.coreFeatures = coreFeatures;
        }

        public List<String> getExcludedFeatures() {
            return excludedFeatures;
        }

        public void setExcludedFeatures(List<String> excludedFeatures) {
            this.excludedFeatures = excludedFeatures;
        }

        public List<String> getValidationPlan() {
            return validationPlan;
        }

        public void setValidationPlan(List<String> validationPlan) {
            this.validationPlan = validationPlan;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
