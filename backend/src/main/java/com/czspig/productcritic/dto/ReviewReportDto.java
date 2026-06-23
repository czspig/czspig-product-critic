package com.czspig.productcritic.dto;

import java.util.ArrayList;
import java.util.List;

public class ReviewReportDto {

    private String oneLineVerdict;
    private Integer beatScore;
    private Integer positioningScore;
    private String painPointAnalysis;
    private List<String> fakeDemandRisks = new ArrayList<>();
    private List<String> featureRedundancyCheck = new ArrayList<>();
    private List<String> coldStartProblems = new ArrayList<>();
    private List<String> mvpSuggestions = new ArrayList<>();
    private MinimumBuildVersion minimumBuildVersion;
    private String developerPrompt;

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

    public String getPainPointAnalysis() {
        return painPointAnalysis;
    }

    public void setPainPointAnalysis(String painPointAnalysis) {
        this.painPointAnalysis = painPointAnalysis;
    }

    public List<String> getFakeDemandRisks() {
        return fakeDemandRisks;
    }

    public void setFakeDemandRisks(List<String> fakeDemandRisks) {
        this.fakeDemandRisks = fakeDemandRisks;
    }

    public List<String> getFeatureRedundancyCheck() {
        return featureRedundancyCheck;
    }

    public void setFeatureRedundancyCheck(List<String> featureRedundancyCheck) {
        this.featureRedundancyCheck = featureRedundancyCheck;
    }

    public List<String> getColdStartProblems() {
        return coldStartProblems;
    }

    public void setColdStartProblems(List<String> coldStartProblems) {
        this.coldStartProblems = coldStartProblems;
    }

    public List<String> getMvpSuggestions() {
        return mvpSuggestions;
    }

    public void setMvpSuggestions(List<String> mvpSuggestions) {
        this.mvpSuggestions = mvpSuggestions;
    }

    public MinimumBuildVersion getMinimumBuildVersion() {
        return minimumBuildVersion;
    }

    public void setMinimumBuildVersion(MinimumBuildVersion minimumBuildVersion) {
        this.minimumBuildVersion = minimumBuildVersion;
    }

    public String getDeveloperPrompt() {
        return developerPrompt;
    }

    public void setDeveloperPrompt(String developerPrompt) {
        this.developerPrompt = developerPrompt;
    }

    public static class MinimumBuildVersion {

        private String goal;
        private List<String> coreFeatures = new ArrayList<>();
        private List<String> excludedFeatures = new ArrayList<>();

        public String getGoal() {
            return goal;
        }

        public void setGoal(String goal) {
            this.goal = goal;
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
    }
}
