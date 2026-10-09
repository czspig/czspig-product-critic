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
  withCredentials: true,
});

let csrf = '';
export function setCsrf(value: string) { csrf = value; }
http.interceptors.request.use((config) => {
  (config as typeof config & { ownerKey?: string }).ownerKey = getSessionId();
  if (csrf) config.headers.set('X-CSRF-Token', csrf);
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
    console.error('Review API request failed', JSON.stringify({
      message: error.message,
      status: error.response?.status,
      code: data?.code,
      url: error.config?.url,
      method: error.config?.method,
    }));
    return data?.message || '提供评审服务时暂时遇到问题，请稍后重试。';
  }
  if (error instanceof Error) {
    console.error('Review request failed', error);
    return error.message;
  }
  return '请求失败，请稍后再试';
}

http.interceptors.response.use(response => {
  if ((response.config as typeof response.config & { ownerKey?: string }).ownerKey !== getSessionId()) throw new axios.CanceledError('身份已切换，请重新打开当前页面');
  if (response.config.method !== 'get') window.dispatchEvent(new Event('projects-changed'));
  return response;
}, async error => {
  if (error.config?.ownerKey && error.config.ownerKey !== getSessionId()) return Promise.reject(new axios.CanceledError('身份已切换，请重新打开当前页面'));
  if (error.response?.status === 403 && error.response?.data?.code === 'FORBIDDEN' && !error.config?.csrfRetried) {
    const { account, loadSession } = await import('@/stores/account');
    const oldIdentity = account.identityKey;
    await loadSession();
    if (oldIdentity !== account.identityKey) { window.dispatchEvent(new Event('account-expired')); return Promise.reject(error); }
    // The server rejected this request before executing it. Retry once with the renewed CSRF token.
    error.config.csrfRetried = true;
    return http.request(error.config);
  }
  if (error.response?.status === 401) window.dispatchEvent(new Event('account-expired'));
  return Promise.reject(error);
});
