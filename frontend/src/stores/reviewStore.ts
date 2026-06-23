import { defineStore } from 'pinia';

import { toErrorMessage } from '@/api/http';
import * as reviewApi from '@/api/reviewApi';
import type {
  CreateReviewPayload,
  PageResponse,
  ReviewDetailResponse,
  ReviewListItemResponse,
} from '@/types/review';

interface ReviewState {
  currentReview: ReviewDetailResponse | null;
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
  },
});
