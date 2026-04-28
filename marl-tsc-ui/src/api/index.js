import axios from 'axios'

const api = axios.create({
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// Response interceptor: unwrap ApiResult
api.interceptors.response.use(
  res => {
    const body = res.data
    if (body.code !== undefined && body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data !== undefined ? body.data : body
  },
  err => {
    const msg = err.response?.data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

// === Web Gateway (port 8080) ===
export const webApi = {
  getDashboard: (intersectionId) => api.get(`/api/v1/web/dashboard/${intersectionId}`),
  listIntersections: () => api.get('/api/v1/web/intersections'),
  triggerOptimize: (data) => api.post('/api/v1/web/optimize', data),
  getOptimizeStatus: (resultId) => api.get(`/api/v1/web/optimize/status/${resultId}`)
}

// === DRL Engine (port 8084) ===
export const drlApi = {
  decide: (data) => api.post('/api/v1/drl/decide', data),
  train: (data) => api.post('/api/v1/drl/train', data),
  listAgents: () => api.get('/api/v1/drl/agents'),
  getAgent: (id) => api.get(`/api/v1/drl/agents/${id}`),
  toggleTraining: (id) => api.post(`/api/v1/drl/agents/${id}/toggle-training`),
  resetAgent: (id) => api.post(`/api/v1/drl/agents/${id}/reset`)
}

// === Signal Control (port 8081) ===
export const signalApi = {
  getPlan: (intersectionId) => api.get(`/api/v1/signal/plans/${intersectionId}`),
  adjustPhase: (data) => api.post('/api/v1/signal/plans/adjust', data)
}

// === Route Planning (port 8082) ===
export const routeApi = {
  planRoute: (data) => api.post('/api/v1/route/plan', data),
  getAlternatives: (data) => api.post('/api/v1/route/alternatives', data),
  getNetworkStatus: () => api.get('/api/v1/route/network/status')
}

// === Data Fusion (port 8085) ===
export const fusionApi = {
  getFeatures: (id) => api.get(`/api/v1/fusion/features/${id}`),
  getState: (id) => api.get(`/api/v1/fusion/state/${id}`),
  ingest: (data) => api.post('/api/v1/fusion/ingest', data),
  // Simulation endpoints (real-time traffic simulation engine)
  listIntersectionInfo: () => api.get('/api/v1/fusion/simulation/intersections'),
  getAllSnapshots: () => api.get('/api/v1/fusion/simulation/all'),
  getSnapshot: (id) => api.get(`/api/v1/fusion/simulation/${id}`),
  getIntersectionInfo: (id) => api.get(`/api/v1/fusion/simulation/intersection/${id}`)
}

// === Co-Optimization (port 8083) ===
export const cooptApi = {
  optimize: (data) => api.post('/api/v1/coopt/optimize', data),
  getResults: (id) => api.get(`/api/v1/coopt/results/${id}`)
}

// === Stream (port 8086) ===
export const streamApi = {
  getStatus: () => api.get('/api/v1/stream/status'),
  start: () => api.post('/api/v1/stream/start'),
  stop: () => api.post('/api/v1/stream/stop'),
  getMetrics: () => api.get('/api/v1/stream/metrics')
}

export default api
