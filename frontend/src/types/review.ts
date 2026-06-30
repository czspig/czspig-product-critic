export type ReviewMode = 'MENTOR' | 'SHARP_PM' | 'CLIENT';
export type ReviewStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
export type GoDecision = 'CONTINUE' | 'PIVOT' | 'PAUSE';
export type ReviewTargetType = 'NEW_IDEA' | 'MATURE_PRODUCT' | 'CLIENT_REQUIREMENT' | 'UNCLEAR';

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
  createdAt: string;
}

export interface ReviewGroupResponse {
  ideaGroupId: string;
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
