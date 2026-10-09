import { reactive } from 'vue';
import { http, setCsrf, unwrap } from '@/api/http';
import { setSessionIdentity } from '@/utils/session';
export interface User { id: number; username: string; nickname: string; email: string; verified: boolean }
export const account = reactive({ user: null as User | null, identityKey: '', anonymousKey: '', remaining: 3, anonymousProjects: 0, ready: false });
interface Session { user: Partial<User>; csrf: string; identityKey: string; anonymousKey: string; remaining: number; anonymousProjects: number }
export function applySession(s: Session) {
  account.user = s.user.id ? s.user as User : null;
  account.identityKey = s.identityKey; account.anonymousKey = s.anonymousKey;
  account.remaining = s.remaining; account.anonymousProjects = s.anonymousProjects;
  setSessionIdentity(s.identityKey); setCsrf(s.csrf); account.ready = true;
}
export async function loadSession() { applySession(unwrap((await http.get('/account/session')).data) as Session); }
export function clearAccountDrafts(key = account.identityKey) {
  if (!key.startsWith('account:')) return;
  for (const k of Object.keys(localStorage)) if (k.startsWith(`czspig-draft-v2:${key}:`)) localStorage.removeItem(k);
}
export function broadcastAccountChange(action = 'identity') { localStorage.setItem('czspig-account-event', `${action}:${Date.now()}:${Math.random()}`); }
export async function login(username: string, password: string) {
  clearAccountDrafts(); applySession(unwrap((await http.post('/account/login', { username, password })).data) as Session); broadcastAccountChange();
}
export async function logout() {
  await http.post('/account/logout');
  let failed = false;
  try { clearAccountDrafts(); } catch { failed = true; }
  try { broadcastAccountChange(); } catch { }
  await loadSession();
  if (failed) throw new Error('已退出登录，但本地草稿清理失败，请检查浏览器存储设置。账号内容已隐藏。');
}
export async function migrateProjects() {
  const data = unwrap((await http.post('/account/migrate')).data) as { projects: string[] };
  for (const key of Object.keys(localStorage)) {
    const prefix = `czspig-draft-v2:${account.anonymousKey}:`;
    if (!key.startsWith(prefix)) continue;
    const rest = key.slice(prefix.length);
    if (data.projects.some(id => rest.startsWith(`${id}:`))) {
      const target = `czspig-draft-v2:${account.identityKey}:${rest}`;
      const raw = localStorage.getItem(key)!;
      if (localStorage.getItem(target) && localStorage.getItem(target) !== raw) throw new Error('项目已加入账号，但存在不同内容的账号草稿。原匿名草稿仍保留，请先备份后再处理。');
      localStorage.setItem(target, raw);
      if (localStorage.getItem(target) !== raw) throw new Error('项目已加入账号，但草稿保存失败，原匿名草稿仍保留。请重新尝试迁移。');
      localStorage.removeItem(key);
    }
  }
  await loadSession(); broadcastAccountChange('projects'); window.dispatchEvent(new Event('projects-changed'));
}
export function safeReturn(value: unknown) {
  if (value === '/' || (typeof value === 'string' && /^\/\?result=[1-9]\d*$/.test(value))) return value;
  if (typeof value === 'string' && /^\/(?:projects|ideas|reviews|drafts)\/[A-Za-z0-9_-]+(?:\?[^\\]*)?$/.test(value) && !/[\\\r\n]/.test(value)) return value;
  if (typeof value === 'string' && /^\/history(?:\?page=[1-9]\d*)?$/.test(value)) return value;
  return '/projects';
}
