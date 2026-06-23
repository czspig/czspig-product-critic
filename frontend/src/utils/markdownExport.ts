import type { ReviewDetailResponse } from '@/types/review';

export function buildMarkdownFilename(review: ReviewDetailResponse) {
  const date = review.createdAt?.slice(0, 10).replaceAll('-', '') || 'review';
  return `czspig-product-review-${date}-${review.id}.md`;
}

export function downloadMarkdown(review: ReviewDetailResponse) {
  const content = review.reportMarkdown || `# 猪猪产品毒舌官评审报告\n\n${review.oneLineVerdict}`;
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = buildMarkdownFilename(review);
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}
