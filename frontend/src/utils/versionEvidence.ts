import type { ReviewVersionItem } from '@/types/review';

export function compareVersionEvidence(current: ReviewVersionItem, versions: ReviewVersionItem[]) {
  const parent = versions.find(v => v.id === current.parentReviewId);
  if (!parent) return { parent: null, added: [], removed: [], persistentRisks: [], omittedRisks: [], newRisks: [], unavailable: true };
  const chunks = (text?: string) => [...new Set((text || '').split(/[。！？\n]+/).map(s => s.trim()).filter(Boolean))];
  const before = chunks(parent.inputContent), after = chunks(current.inputContent);
  const riskTitles = (v: ReviewVersionItem) => v.validationGuide?.risks?.map(r => r.title) || v.criticalHypotheses || [];
  const oldRisks = riskTitles(parent), newRisks = riskTitles(current);
  return {
    parent, unavailable: !parent.inputContent || !current.inputContent,
    added: after.filter(s => !before.includes(s)), removed: before.filter(s => !after.includes(s)),
    persistentRisks: newRisks.filter(r => oldRisks.includes(r)), omittedRisks: oldRisks.filter(r => !newRisks.includes(r)),
    newRisks: newRisks.filter(r => !oldRisks.includes(r)),
  };
}
