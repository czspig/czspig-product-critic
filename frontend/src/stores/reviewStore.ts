import { defineStore } from 'pinia';

import { toErrorMessage } from '@/api/http';
import * as reviewApi from '@/api/reviewApi';
import type {
  CreateReviewPayload,
  PageResponse,
  ReviewDetailResponse,
  ReviewGroupResponse,
  ReviewListItemResponse,
} from '@/types/review';

interface DraftContext {
  content: string;
  ideaGroupId: string;
  parentReviewId: number;
  parentVersionNo: number;
  nextVersionNo: number;
}

interface ReviewState {
  currentReview: ReviewDetailResponse | null;
  currentGroup: ReviewGroupResponse | null;
  draftContext: DraftContext | null;
  history: ReviewListItemResponse[];
  historyTotal: number;
  historyPage: number;
  loading: boolean;
  historyLoading: boolean;
  error: string;
}

export const useReviewStore = defineStore('review', {
  state: (): ReviewState => ({
    currentReview: null,
    currentGroup: null,
    draftContext: null,
    history: [],
    historyTotal: 0,
    historyPage: 1,
    loading: false,
    historyLoading: false,
    error: '',
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
    prepareDraftFromReview(review: ReviewDetailResponse, content: string) {
      const parentVersionNo = review.versionNo || 1;
      this.draftContext = {
        content,
        ideaGroupId: review.ideaGroupId || String(review.id),
        parentReviewId: review.id,
        parentVersionNo,
        nextVersionNo: parentVersionNo + 1,
      };
    },
    clearDraftContext() {
      this.draftContext = null;
    },
  },
});
