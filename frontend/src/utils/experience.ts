import { getSessionId } from '@/utils/session';
import type { CreateReviewPayload, ProjectWorkbenchResponse } from '@/types/review';

// Existing title is an input summary, not a user-assigned product name.
// Keep a compact, stable excerpt from V1; do not treat the entire idea as a title.
export function projectName(title?: string) {
  const text = title?.trim() || '';
  const firstLine = text.split(/[\n。]/)[0].replace(/^我想做一个|^我想做|^我想/, '').trim();
  return firstLine ? firstLine.slice(0, 22) + (firstLine.length > 22 ? '…' : '') : '产品想法项目';
}
export async function projectDisplayName(project: ProjectWorkbenchResponse) {
  return projectName(project.title);
}
export function draftKey(group?: string, sourceId?: number) {
  return `czspig-draft-v2:${getSessionId()}:${group || 'new'}:${sourceId || 'new'}`;
}
export function readSavedDraft(key: string): CreateReviewPayload | null {
  const raw = localStorage.getItem(key);
  if (!raw) return null;
  const value = JSON.parse(raw);
  if (typeof value.content !== 'string' || value.content.length > 5000 ||
      !['MENTOR', 'SHARP_PM', 'CLIENT'].includes(value.mode) || ![1, 2, 3].includes(value.roastLevel)) {
    throw new Error('草稿内容不完整，无法恢复');
  }
  return { content: value.content, mode: value.mode, roastLevel: value.roastLevel };
}
export function writeSavedDraft(key: string, value: CreateReviewPayload) {
  const raw = JSON.stringify({ content: value.content, mode: value.mode, roastLevel: value.roastLevel });
  localStorage.setItem(key, raw);
  if (localStorage.getItem(key) !== raw) throw new Error('草稿未能保存');
}
export function reportReturn(from: unknown, groupId: string) {
  if (typeof from !== 'string') return { path: `/projects/${groupId}`, label: '返回项目工作台' };
  if (/^\/history(?:\?page=[1-9]\d*)?$/.test(from)) return { path: from, label: '返回历史记录' };
  if (from === `/ideas/${groupId}`) return { path: from, label: '返回版本对比' };
  if (from === `/projects/${groupId}`) return { path: from, label: '返回项目工作台' };
  return { path: `/projects/${groupId}`, label: '返回项目工作台' };
}
