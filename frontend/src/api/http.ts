import axios from 'axios';

import type { ApiResponse } from '@/types/review';
import { getSessionId } from '@/utils/session';

export class ApiError extends Error {
  code: string;

  constructor(code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
  }
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 120000,
});

http.interceptors.request.use((config) => {
  config.headers.set('X-Session-Id', getSessionId());
  return config;
});

export function unwrap<T>(response: ApiResponse<T>) {
  if (!response.success) {
    throw new ApiError(response.code, response.message);
  }
  return response.data;
}

export function toErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Partial<ApiResponse<unknown>> | undefined;
    return data?.message || error.message || '请求失败，请稍后再试';
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '请求失败，请稍后再试';
}
