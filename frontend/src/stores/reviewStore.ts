import { defineStore } from 'pinia';

import { toErrorMessage } from '@/api/http';
import * as reviewApi from '@/api/reviewApi';
import type {
  CreateReviewPayload,
  PageResponse,
  ProjectWorkbenchResponse,
  ReviewDetailResponse,
  ReviewGroupResponse,
  ReviewListItemResponse,
} from '@/types/review';

interface ReviewState {
  currentReview: ReviewDetailResponse | null;
  currentGroup: ReviewGroupResponse | null;
  currentProject: ProjectWorkbenchResponse | null;
  history: ReviewListItemResponse[];
  historyTotal: number;
  historyPage: number;
  loading: boolean;
  historyLoading: boolean;
  projectLoading: boolean;
  error: string;
  projectError: string;
}

export const useReviewStore = defineStore('review', {
  state: (): ReviewState => ({
    currentReview: null,
    currentGroup: null,
    currentProject: null,
    history: [],
    historyTotal: 0,
    historyPage: 1,
    loading: false,
    historyLoading: false,
    projectLoading: false,
    error: '',
    projectError: '',
  }),
  actions: {
    async create(payload: CreateReviewPayload) {
      this.loading = true;
      this.error = '';
      try {
        this.currentReview = await reviewApi.createReview(payload);
        return this.currentReview;
      } catch (error) {
        this.error = toErrorMessage(error);
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async fetchReview(id: number) {
      this.loading = true;
      this.error = '';
      try {
        this.currentReview = await reviewApi.getReview(id);
        return this.currentReview;
      } catch (error) {
        this.error = toErrorMessage(error);
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async fetchHistory(page = 1, pageSize = 10) {
      this.historyLoading = true;
      this.error = '';
      try {
        const result: PageResponse<ReviewListItemResponse> = await reviewApi.listReviews(page, pageSize);
        this.history = result.items;
        this.historyTotal = result.total;
        this.historyPage = result.page;
        return result;
      } catch (error) {
        this.error = toErrorMessage(error);
        throw error;
      } finally {
        this.historyLoading = false;
      }
    },
    async fetchGroup(ideaGroupId: string) {
      this.loading = true;
      this.error = '';
      this.currentGroup = null;
      try {
        this.currentGroup = await reviewApi.getReviewGroup(ideaGroupId);
        return this.currentGroup;
      } catch (error) {
        this.error = toErrorMessage(error);
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async fetchProject(ideaGroupId: string) {
      this.projectLoading = true;
      this.projectError = '';
      this.currentProject = null;
      try {
        this.currentProject = await reviewApi.getProjectWorkbench(ideaGroupId);
        return this.currentProject;
      } catch (error) {
        this.projectError = toErrorMessage(error);
        throw error;
      } finally {
        this.projectLoading = false;
      }
    },
    clearProject() {
      this.currentProject = null;
      this.projectError = '';
    },
  },
});
