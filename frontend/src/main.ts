import { createPinia } from 'pinia';
import { createApp } from 'vue';

import App from './App.vue';
import router from './router';
import './styles/global.css';

import { loadSession, account, clearAccountDrafts } from '@/stores/account';
let exiting = false;
function expire() {
  if (exiting) return;
  exiting = true; try { clearAccountDrafts(); } catch { }
  window.location.replace('/account/login?expired=1&returnTo=' + encodeURIComponent(window.location.pathname + window.location.search));
}
window.addEventListener('account-expired', expire);
window.addEventListener('storage', event => {
  if (event.key === 'czspig-account-event') { try { if (!event.newValue?.startsWith('projects:')) clearAccountDrafts(); } catch { } window.location.replace('/projects'); }
});
window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });
window.addEventListener('focus', async () => {
  const old = account.identityKey;
  if (!account.ready || exiting) return;
  try { await loadSession(); if (old !== account.identityKey) { clearAccountDrafts(old); window.location.replace('/projects'); } } catch { }
});
loadSession().then(() => createApp(App).use(createPinia()).use(router).mount('#app')).catch(() => {
  const root = document.getElementById('app');
  if (root) { root.textContent = '暂时无法连接服务，请确认后端已启动后刷新页面。'; const retry = document.createElement('button'); retry.textContent = '重新连接'; retry.onclick = () => window.location.reload(); root.append(retry); }
});
