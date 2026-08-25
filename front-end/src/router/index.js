import { createRouter, createWebHistory } from 'vue-router'

const Layout = () => import('@/layout/Layout.vue')

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: Layout,
    redirect: '/students',
    children: [
      { path: 'students', name: '学员管理', component: () => import('@/views/Students.vue'), meta: { icon: 'User' } },
      { path: 'courses', name: '课程管理', component: () => import('@/views/Courses.vue'), meta: { icon: 'Reading' } },
      { path: 'teachers', name: '教师管理', component: () => import('@/views/Teachers.vue'), meta: { icon: 'Avatar' } },
      { path: 'schedules', name: '排课管理', component: () => import('@/views/Schedules.vue'), meta: { icon: 'Calendar' } },
      { path: 'bookings', name: '约课管理', component: () => import('@/views/Bookings.vue'), meta: { icon: 'Tickets' } },
      { path: 'checkins', name: '签到管理', component: () => import('@/views/Checkins.vue'), meta: { icon: 'CircleCheck' } },
      { path: 'banners', name: 'Banner 管理', component: () => import('@/views/Banners.vue'), meta: { icon: 'Picture' } },
      { path: 'videos', name: '视频管理', component: () => import('@/views/Videos.vue'), meta: { icon: 'VideoCamera' } },
      { path: 'qa-management', name: '问答管理', component: () => import('@/views/QaManagement.vue'), meta: { icon: 'ChatDotRound' } },
      { path: 'settings', name: '系统设置', component: () => import('@/views/Settings.vue'), meta: { icon: 'Setting' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.meta.public) return next()
  if (!token) return next('/login')
  next()
})

export default router
