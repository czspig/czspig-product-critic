import type { ReviewMode, ReviewStatus } from '@/types/review';

export const modeLabels: Record<ReviewMode, string> = {
  MENTOR: '导师模式',
  SHARP_PM: '毒舌 PM',
  CLIENT: '甲方视角',
};

export const statusLabels: Record<ReviewStatus, string> = {
  PENDING: '生成中',
  SUCCESS: '已完成',
  FAILED: '失败',
};
