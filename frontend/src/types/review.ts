export type ReviewMode = 'MENTOR' | 'SHARP_PM' | 'CLIENT';
export type ReviewStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
export type GoDecision = 'CONTINUE' | 'PIVOT' | 'PAUSE';
export type ReviewTargetType = 'NEW_IDEA' | 'MATURE_PRODUCT' | 'CLIENT_REQUIREMENT' | 'UNCLEAR';
export type ConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW';
export type ScoreDirection = 'HIGHER_IS_BETTER' | 'HIGHER_IS_WORSE';
export type OverallDecision = 'PROCEED' | 'VALIDATE_FIRST' | 'NARROW_SCOPE' | 'PAUSE';
export type QualityStatus = 'PASS' | 'WARNING' | 'FAIL';

export interface CreateReviewPayload {
  content: string;
  mode: ReviewMode;
  roastLevel: number;
  ideaGroupId?: string;
  parentReviewId?: number;
}

export interface MinimumBuildVersion {
  goal: string;
  coreFeatures: string[];
  excludedFeatures: string[];
  successMetric?: string;
  validationPlan?: string[];
}

export interface OverallAssessment {
  overallScore?: number;
  decision?: OverallDecision;
  strongestDimension?: string;
  weakestDimension?: string;
  mostUncertainDimension?: string;
  priorityQuestion?: string;
  overallReasoning?: string;
}

export interface DimensionScore {
  dimensionKey: string;
  dimensionName: string;
  score: number;
  confidence: ConfidenceLevel;
  scoreDirection: ScoreDirection;
  summary: string;
  evidence: string[];
  deductions: string[];
  missingInformation: string[];
  improvementActions: string[];
  changeReason?: string;
}

export interface ProvidedFact {
  statement: string;
  sourceText: string;
}

export interface Inference {
  statement: string;
  basis?: string;
  confidence?: ConfidenceLevel;
}

export interface CriticalHypothesis {
  statement: string;
  importance?: ConfidenceLevel;
  impactIfFalse?: string;
  relatedDimensions?: string[];
}

export interface UnknownQuestion {
  question: string;
  reason?: string;
}

export interface KnowledgeBoundary {
  providedFacts: ProvidedFact[];
  inferences: Inference[];
  criticalHypotheses: CriticalHypothesis[];
  unknowns: UnknownQuestion[];
}

export interface QualityCheck {
  key: string;
  name: string;
  status: QualityStatus;
  message: string;
  relatedPaths?: string[];
}

export interface QualityIssue {
  severity?: ConfidenceLevel;
  type?: string;
  message: string;
  suggestion?: string;
}

export interface QualityAudit {
  overallStatus?: QualityStatus;
  checks: QualityCheck[];
  issues: QualityIssue[];
  summary?: string;
}

export interface ReviewReport {
  oneLineVerdict: string;
  reviewTargetType?: ReviewTargetType;
  goDecision?: GoDecision;
  goDecisionReason?: string;
  beatScore: number;
  positioningScore: number;
  painPointAnalysis: string;
  fakeDemandRisks: string[];
  featureRedundancyCheck: string[];
  coldStartProblems: string[];
  mvpSuggestions: string[];
  minimumBuildVersion: MinimumBuildVersion;
  developerPrompt: string;
  overallAssessment?: OverallAssessment;
  dimensionScores?: DimensionScore[];
  knowledgeBoundary?: KnowledgeBoundary;
  qualityAudit?: QualityAudit;
}

export interface ReviewDetailResponse {
  id: number;
  inputContent: string;
  inputSummary: string;
  ideaGroupId: string;
  versionNo: number;
  parentReviewId?: number | null;
  groupVersionCount: number;
  mode: ReviewMode;
  roastLevel: number;
  oneLineVerdict: string;
  beatScore: number;
  positioningScore: number;
  report: ReviewReport;
  reportJson: string;
  reportMarkdown: string;
  status: ReviewStatus;
  errorMessage?: string | null;
  providerName?: string;
  modelName?: string;
  fallbackUsed?: boolean;
  createdAt: string;
}

export interface ReviewListItemResponse {
  id: number;
  inputSummary: string;
  ideaGroupId: string;
  versionNo: number;
  parentReviewId?: number | null;
  groupVersionCount: number;
  mode: ReviewMode;
  roastLevel: number;
  oneLineVerdict: string;
  beatScore: number;
  positioningScore: number;
  status: ReviewStatus;
  errorMessage?: string | null;
  createdAt: string;
}

export interface ReviewVersionItem {
  id: number;
  versionNo: number;
  parentReviewId?: number | null;
  goDecision?: GoDecision;
  goDecisionReason?: string;
  beatScore: number;
  positioningScore: number;
  oneLineVerdict: string;
  successMetric?: string;
  minimumBuildGoal?: string;
  coreFeatures: string[];
  excludedFeatures: string[];
  validationPlan: string[];
  scoreChangeReason?: string;
  confidenceSummary?: string;
  criticalHypotheses?: string[];
  qualityStatus?: QualityStatus;
  createdAt: string;
}

export interface ReviewGroupResponse {
  ideaGroupId: string;
  versions: ReviewVersionItem[];
}

export interface ProjectWorkbenchResponse {
  projectId: string;
  title: string;
  latestReviewId: number;
  latestVersionNo: number;
  updatedAt: string;
  totalVersions: number;
  decision?: GoDecision;
  decisionReason?: string;
  oneLineVerdict?: string;
  beatScore?: number;
  positioningScore?: number;
  overallScore?: number;
  qualityStatus?: QualityStatus;
  qualitySummary?: string;
  providedFacts: ProvidedFact[];
  inferences: Inference[];
  criticalHypotheses: CriticalHypothesis[];
  unknownQuestions: UnknownQuestion[];
  successMetric?: string;
  validationPlan: string[];
  versions: ReviewVersionItem[];
}

export interface PageResponse<T> {
  page: number;
  pageSize: number;
  total: number;
  items: T[];
}

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}
