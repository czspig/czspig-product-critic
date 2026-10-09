import { createRouter, createWebHistory } from 'vue-router';

import HistoryPage from '@/pages/HistoryPage.vue';
import HomePage from '@/pages/HomePage.vue';
import IdeaVersionsPage from '@/pages/IdeaVersionsPage.vue';
import NotFoundPage from '@/pages/NotFoundPage.vue';
import ProjectWorkbenchPage from '@/pages/ProjectWorkbenchPage.vue';
import ReviewResultPage from '@/pages/ReviewResultPage.vue';
import AccountPage from '@/pages/AccountPage.vue';
import ProjectsPage from '@/pages/ProjectsPage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/account/:mode', component: AccountPage },
    { path: '/projects', component: ProjectsPage },
    { path: '/', name: 'home', component: HomePage },
    { path: '/drafts/:sourceId', name: 'iteration-draft', component: HomePage },
    { path: '/reviews/:id', name: 'review-detail', component: ReviewResultPage, props: true },
    { path: '/ideas/:groupId', name: 'idea-versions', component: IdeaVersionsPage, props: true },
    { path: '/projects/:groupId', name: 'project-workbench', component: ProjectWorkbenchPage, props: true },
    { path: '/history', name: 'history', component: HistoryPage },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundPage },
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition;
    if (to.hash) return { el: to.hash, top: 84 };
    return { top: 0 };
  },
});

export default router;
