import { createRouter, createWebHistory } from 'vue-router';

import HistoryPage from '@/pages/HistoryPage.vue';
import HomePage from '@/pages/HomePage.vue';
import IdeaVersionsPage from '@/pages/IdeaVersionsPage.vue';
import NotFoundPage from '@/pages/NotFoundPage.vue';
import ReviewResultPage from '@/pages/ReviewResultPage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomePage },
    { path: '/reviews/:id', name: 'review-detail', component: ReviewResultPage, props: true },
    { path: '/ideas/:groupId', name: 'idea-versions', component: IdeaVersionsPage, props: true },
    { path: '/history', name: 'history', component: HistoryPage },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundPage },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

export default router;
