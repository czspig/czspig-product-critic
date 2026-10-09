package com.czspig.productcritic.dto;

import java.util.ArrayList;
import java.util.List;

public class ReviewReportDto {

    private String oneLineVerdict;
    private String reviewTargetType;
    private String goDecision;
    private String goDecisionReason;
    private Integer beatScore;
    private Integer positioningScore;
    private String painPointAnalysis;
    private List<String> fakeDemandRisks = new ArrayList<>();
    private List<String> featureRedundancyCheck = new ArrayList<>();
    private List<String> coldStartProblems = new ArrayList<>();
    private List<String> mvpSuggestions = new ArrayList<>();
    private MinimumBuildVersion minimumBuildVersion;
    private String developerPrompt;
    private OverallAssessment overallAssessment;
    private List<DimensionScore> dimensionScores = new ArrayList<>();
    private KnowledgeBoundary knowledgeBoundary;
    private QualityAudit qualityAudit;
    private ValidationGuide validationGuide;

    public ValidationGuide getValidationGuide() { return validationGuide; }
    public void setValidationGuide(ValidationGuide validationGuide) { this.validationGuide = validationGuide; }
    public record ValidationGuide(boolean informationInsufficient, String decisionReason,
            List<RiskExperiment> risks, String continueWhen, String adjustWhen, String pauseWhen) {}
    public record RiskExperiment(String title, String priorityReason, String evidenceType, String basis,
            String impact, String target, List<String> steps, List<String> evidenceToCollect,
            String referenceThreshold, String thresholdReason) {}

    public String getOneLineVerdict() {
        return oneLineVerdict;
    }

    public void setOneLineVerdict(String oneLineVerdict) {
        this.oneLineVerdict = oneLineVerdict;
    }

    public String getReviewTargetType() {
        return reviewTargetType;
    }

    public void setReviewTargetType(String reviewTargetType) {
        this.reviewTargetType = reviewTargetType;
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

    public OverallAssessment getOverallAssessment() {
        return overallAssessment;
    }

    public void setOverallAssessment(OverallAssessment overallAssessment) {
        this.overallAssessment = overallAssessment;
    }

    public List<DimensionScore> getDimensionScores() {
        return dimensionScores;
    }

    public void setDimensionScores(List<DimensionScore> dimensionScores) {
        this.dimensionScores = dimensionScores;
    }

    public KnowledgeBoundary getKnowledgeBoundary() {
        return knowledgeBoundary;
    }

    public void setKnowledgeBoundary(KnowledgeBoundary knowledgeBoundary) {
        this.knowledgeBoundary = knowledgeBoundary;
    }

    public QualityAudit getQualityAudit() {
        return qualityAudit;
    }

    public void setQualityAudit(QualityAudit qualityAudit) {
        this.qualityAudit = qualityAudit;
    }

    public static class OverallAssessment {

        private Integer overallScore;
        private String decision;
        private String strongestDimension;
        private String weakestDimension;
        private String mostUncertainDimension;
        private String priorityQuestion;
        private String overallReasoning;

        public Integer getOverallScore() {
            return overallScore;
        }

        public void setOverallScore(Integer overallScore) {
            this.overallScore = overallScore;
        }

        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public String getStrongestDimension() {
            return strongestDimension;
        }

        public void setStrongestDimension(String strongestDimension) {
            this.strongestDimension = strongestDimension;
        }

        public String getWeakestDimension() {
            return weakestDimension;
        }

        public void setWeakestDimension(String weakestDimension) {
            this.weakestDimension = weakestDimension;
        }

        public String getMostUncertainDimension() {
            return mostUncertainDimension;
        }

        public void setMostUncertainDimension(String mostUncertainDimension) {
            this.mostUncertainDimension = mostUncertainDimension;
        }

        public String getPriorityQuestion() {
            return priorityQuestion;
        }

        public void setPriorityQuestion(String priorityQuestion) {
            this.priorityQuestion = priorityQuestion;
        }

        public String getOverallReasoning() {
            return overallReasoning;
        }

        public void setOverallReasoning(String overallReasoning) {
            this.overallReasoning = overallReasoning;
        }
    }

    public static class DimensionScore {

        private String dimensionKey;
        private String dimensionName;
        private Integer score;
        private String confidence;
        private String scoreDirection;
        private String summary;
        private List<String> evidence = new ArrayList<>();
        private List<String> deductions = new ArrayList<>();
        private List<String> missingInformation = new ArrayList<>();
        private List<String> improvementActions = new ArrayList<>();
        private String changeReason;

        public String getDimensionKey() {
            return dimensionKey;
        }

        public void setDimensionKey(String dimensionKey) {
            this.dimensionKey = dimensionKey;
        }

        public String getDimensionName() {
            return dimensionName;
        }

        public void setDimensionName(String dimensionName) {
            this.dimensionName = dimensionName;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public String getScoreDirection() {
            return scoreDirection;
        }

        public void setScoreDirection(String scoreDirection) {
            this.scoreDirection = scoreDirection;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getEvidence() {
            return evidence;
        }

        public void setEvidence(List<String> evidence) {
            this.evidence = evidence;
        }

        public List<String> getDeductions() {
            return deductions;
        }

        public void setDeductions(List<String> deductions) {
            this.deductions = deductions;
        }

        public List<String> getMissingInformation() {
            return missingInformation;
        }

        public void setMissingInformation(List<String> missingInformation) {
            this.missingInformation = missingInformation;
        }

        public List<String> getImprovementActions() {
            return improvementActions;
        }

        public void setImprovementActions(List<String> improvementActions) {
            this.improvementActions = improvementActions;
        }

        public String getChangeReason() {
            return changeReason;
        }

        public void setChangeReason(String changeReason) {
            this.changeReason = changeReason;
        }
    }

    public static class KnowledgeBoundary {

        private List<ProvidedFact> providedFacts = new ArrayList<>();
        private List<Inference> inferences = new ArrayList<>();
        private List<CriticalHypothesis> criticalHypotheses = new ArrayList<>();
        private List<UnknownQuestion> unknowns = new ArrayList<>();

        public List<ProvidedFact> getProvidedFacts() {
            return providedFacts;
        }

        public void setProvidedFacts(List<ProvidedFact> providedFacts) {
            this.providedFacts = providedFacts;
        }

        public List<Inference> getInferences() {
            return inferences;
        }

        public void setInferences(List<Inference> inferences) {
            this.inferences = inferences;
        }

        public List<CriticalHypothesis> getCriticalHypotheses() {
            return criticalHypotheses;
        }

        public void setCriticalHypotheses(List<CriticalHypothesis> criticalHypotheses) {
            this.criticalHypotheses = criticalHypotheses;
        }

        public List<UnknownQuestion> getUnknowns() {
            return unknowns;
        }

        public void setUnknowns(List<UnknownQuestion> unknowns) {
            this.unknowns = unknowns;
        }
    }

    public static class ProvidedFact {

        private String statement;
        private String sourceText;

        public String getStatement() {
            return statement;
        }

        public void setStatement(String statement) {
            this.statement = statement;
        }

        public String getSourceText() {
            return sourceText;
        }

        public void setSourceText(String sourceText) {
            this.sourceText = sourceText;
        }
    }

    public static class Inference {

        private String statement;
        private String basis;
        private String confidence;

        public String getStatement() {
            return statement;
        }

        public void setStatement(String statement) {
            this.statement = statement;
        }

        public String getBasis() {
            return basis;
        }

        public void setBasis(String basis) {
            this.basis = basis;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }
    }

    public static class CriticalHypothesis {

        private String statement;
        private String importance;
        private String impactIfFalse;
        private List<String> relatedDimensions = new ArrayList<>();

        public String getStatement() {
            return statement;
        }

        public void setStatement(String statement) {
            this.statement = statement;
        }

        public String getImportance() {
            return importance;
        }

        public void setImportance(String importance) {
            this.importance = importance;
        }

        public String getImpactIfFalse() {
            return impactIfFalse;
        }

        public void setImpactIfFalse(String impactIfFalse) {
            this.impactIfFalse = impactIfFalse;
        }

        public List<String> getRelatedDimensions() {
            return relatedDimensions;
        }

        public void setRelatedDimensions(List<String> relatedDimensions) {
            this.relatedDimensions = relatedDimensions;
        }
    }

    public static class UnknownQuestion {

        private String question;
        private String reason;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class QualityAudit {

        private String overallStatus;
        private List<QualityCheck> checks = new ArrayList<>();
        private List<QualityIssue> issues = new ArrayList<>();
        private String summary;

        public String getOverallStatus() {
            return overallStatus;
        }

        public void setOverallStatus(String overallStatus) {
            this.overallStatus = overallStatus;
        }

        public List<QualityCheck> getChecks() {
            return checks;
        }

        public void setChecks(List<QualityCheck> checks) {
            this.checks = checks;
        }

        public List<QualityIssue> getIssues() {
            return issues;
        }

        public void setIssues(List<QualityIssue> issues) {
            this.issues = issues;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }
    }

    public static class QualityCheck {

        private String key;
        private String name;
        private String status;
        private String message;
        private List<String> relatedPaths = new ArrayList<>();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<String> getRelatedPaths() {
            return relatedPaths;
        }

        public void setRelatedPaths(List<String> relatedPaths) {
            this.relatedPaths = relatedPaths;
        }
    }

    public static class QualityIssue {

        private String severity;
        private String type;
        private String message;
        private String suggestion;

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }

    public static class MinimumBuildVersion {

        private String goal;
        private List<String> coreFeatures = new ArrayList<>();
        private List<String> excludedFeatures = new ArrayList<>();
        private String successMetric;
        private List<String> validationPlan = new ArrayList<>();

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
    }
}
