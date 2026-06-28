export type ReviewMode = 'MENTOR' | 'SHARP_PM' | 'CLIENT';
export type ReviewStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
export type GoDecision = 'CONTINUE' | 'PIVOT' | 'PAUSE';

export interface CreateReviewPayload {
  content: string;
  mode: ReviewMode;
  roastLevel: number;
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
  mode: ReviewMode;
  roastLevel: number;
  oneLineVerdict: string;
  beatScore: number;
  positioningScore: number;
  status: ReviewStatus;
  errorMessage?: string | null;
  createdAt: string;
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
