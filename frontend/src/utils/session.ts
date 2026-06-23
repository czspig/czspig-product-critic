const SESSION_KEY = 'czspig-product-critic-session-id';

export function getSessionId() {
  const existing = localStorage.getItem(SESSION_KEY);
  if (existing) {
    return existing;
  }
  const next = `czspig-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  localStorage.setItem(SESSION_KEY, next);
  return next;
}
