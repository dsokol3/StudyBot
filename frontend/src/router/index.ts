import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'upload',
    component: () => import('@/views/UploadPage.vue'),
    meta: { transition: 'fade-slide' }
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/DashboardPage.vue'),
    meta: { transition: 'fade-slide' }
  },
  {
    path: '/tools/summary',
    name: 'summary',
    component: () => import('@/views/tools/SummaryPage.vue'),
    meta: { transition: 'fade-slide', requiresNotes: true }
  },
  {
    path: '/tools/flashcards',
    name: 'flashcards',
    component: () => import('@/views/tools/FlashcardsPage.vue'),
    meta: { transition: 'fade-slide', requiresNotes: true }
  },
  {
    path: '/tools/questions',
    name: 'questions',
    component: () => import('@/views/tools/QuestionsPage.vue'),
    meta: { transition: 'fade-slide', requiresNotes: true }
  },
  {
    path: '/tools/essay-prompts',
    name: 'essay-prompts',
    component: () => import('@/views/tools/EssayPromptsPage.vue'),
    meta: { transition: 'fade-slide', requiresNotes: true }
  },
  {
    path: '/tools/explanations',
    name: 'explanations',
    component: () => import('@/views/tools/ExplanationsPage.vue'),
    meta: { transition: 'fade-slide', requiresNotes: true }
  },
  {
    path: '/tools/diagrams',
    name: 'diagrams',
    component: () => import('@/views/tools/DiagramsPage.vue'),
    meta: { transition: 'fade-slide', requiresNotes: true }
  },
  {
    path: '/tools/study-plan',
    name: 'study-plan',
    component: () => import('@/views/tools/StudyPlanPage.vue'),
    meta: { transition: 'fade-slide', requiresNotes: true }
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('@/views/ChatPage.vue'),
    meta: { transition: 'fade-slide' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// Navigation guard for notes requirement
router.beforeEach((to, _from, next) => {
  if (to.meta.requiresNotes) {
    const notesData = localStorage.getItem('study-notes')
    const hasNotes = notesData ? JSON.parse(notesData).length > 0 : false
    if (!hasNotes) {
      next({ name: 'upload' })
      return
    }
  }
  next()
})

export default router
