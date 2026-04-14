import { createRouter, createWebHistory } from 'vue-router'

import DashboardView from '../views/DashboardView.vue'
import CalendarView from '../views/CalendarView.vue'
import ImportView from '../views/ImportView.vue'
import ValidateView from '../views/ValidateView.vue'
import DiffView from '../views/DiffView.vue'
import PublishView from '../views/PublishView.vue'
import PlaygroundView from '../views/PlaygroundView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/calendar', name: 'calendar', component: CalendarView },
    { path: '/import', name: 'import', component: ImportView },
    { path: '/validate', name: 'validate', component: ValidateView },
    { path: '/diff', name: 'diff', component: DiffView },
    { path: '/publish', name: 'publish', component: PublishView },
    { path: '/playground', name: 'playground', component: PlaygroundView },
  ],
})

export default router
