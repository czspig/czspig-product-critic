import type { ReviewMode, ReviewStatus, ReviewTargetType } from '@/types/review';

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

export const reviewTargetTypeLabels: Record<ReviewTargetType, string> = {
  NEW_IDEA: '新产品想法',
  MATURE_PRODUCT: '成熟产品复盘',
  CLIENT_REQUIREMENT: '甲方需求',
  UNCLEAR: '输入不清晰',
};
