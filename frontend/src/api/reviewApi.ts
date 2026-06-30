import { http, unwrap } from '@/api/http';
import type {
  ApiResponse,
  CreateReviewPayload,
  PageResponse,
  ReviewDetailResponse,
  ReviewGroupResponse,
  ReviewListItemResponse,
} from '@/types/review';

export async function createReview(payload: CreateReviewPayload) {
  const response = await http.post<ApiResponse<ReviewDetailResponse>>('/reviews', payload);
  return unwrap(response.data);
}

export async function getReview(id: number) {
  const response = await http.get<ApiResponse<ReviewDetailResponse>>(`/reviews/${id}`);
  return unwrap(response.data);
}

export async function listReviews(page = 1, pageSize = 10) {
  const response = await http.get<ApiResponse<PageResponse<ReviewListItemResponse>>>('/reviews', {
    params: { page, pageSize },
  });
  return unwrap(response.data);
}

export async function getReviewGroup(ideaGroupId: string) {
  const response = await http.get<ApiResponse<ReviewGroupResponse>>(`/reviews/group/${encodeURIComponent(ideaGroupId)}`);
  return unwrap(response.data);
}
