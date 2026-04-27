import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { title: '仪表盘', icon: 'Monitor' }
  },
  {
    path: '/signal',
    name: 'SignalControl',
    component: () => import('@/views/SignalControl.vue'),
    meta: { title: '信号控制', icon: 'Switch' }
  },
  {
    path: '/route',
    name: 'RoutePlanning',
    component: () => import('@/views/RoutePlanning.vue'),
    meta: { title: '路径规划', icon: 'MapLocation' }
  },
  {
    path: '/drl',
    name: 'DrlEngine',
    component: () => import('@/views/DrlEngine.vue'),
    meta: { title: 'DRL引擎', icon: 'Cpu' }
  },
  {
    path: '/monitor',
    name: 'TrafficMonitor',
    component: () => import('@/views/TrafficMonitor.vue'),
    meta: { title: '实时监控', icon: 'VideoCamera' }
  },
  {
    path: '/fusion',
    name: 'DataFusion',
    component: () => import('@/views/DataFusion.vue'),
    meta: { title: '数据融合', icon: 'DataAnalysis' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
