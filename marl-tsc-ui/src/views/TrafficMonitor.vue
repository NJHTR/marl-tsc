<template>
  <div class="traffic-monitor">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <span style="font-weight:600">实时交通流</span>
            <el-tag :type="connected ? 'success' : 'danger'" size="small" style="float:right">
              {{ connected ? '已连接' : '未连接' }}
            </el-tag>
          </template>
          <div ref="liveChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <span style="font-weight:600">实时告警</span>
            <el-button size="small" style="float:right" @click="clearAlerts" type="danger" plain>清空</el-button>
          </template>
          <div class="alert-list">
            <div v-for="(alert, i) in alerts" :key="i" class="alert-item" :class="alert.severity">
              <el-tag :type="severityTag(alert.severity)" size="small" effect="dark">
                {{ alert.alertType }}
              </el-tag>
              <span class="alert-msg">{{ alert.message }}</span>
              <span class="alert-time">{{ formatTime(alert.timestamp) }}</span>
            </div>
            <el-empty v-if="alerts.length === 0" description="暂无告警" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="never" class="section-card">
          <template #header>
            <span style="font-weight:600">路口状态概览</span>
            <el-switch v-model="autoRefresh" active-text="自动刷新" style="float:right;margin-right:16px" />
            <el-button size="small" style="float:right;margin-right:8px" @click="refreshAll">刷新</el-button>
          </template>
          <el-table :data="intersectionStatus" border stripe size="small" v-if="intersectionStatus.length">
            <el-table-column prop="intersectionId" label="路口" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === '拥堵' ? 'danger' : row.status === '缓行' ? 'warning' : 'success'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="flow" label="车流量(辆/时)" width="120" />
            <el-table-column prop="speed" label="速度(km/h)" width="100" />
            <el-table-column prop="occupancy" label="占用率" width="100">
              <template #default="{ row }">
                <el-progress :percentage="Math.round((row.occupancy || 0) * 100)" :color="occColor(row.occupancy)" />
              </template>
            </el-table-column>
            <el-table-column prop="queueLength" label="排队长度(m)" width="100" />
            <el-table-column prop="delay" label="延误(s)" width="80" />
            <el-table-column prop="lastUpdateTime" label="更新时间" />
          </el-table>
          <el-empty v-else description="加载中..." />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { webApi } from '@/api'
import * as echarts from 'echarts'

const connected = ref(false)
const autoRefresh = ref(true)
const alerts = ref([])
const intersectionStatus = ref([])
const liveChartRef = ref(null)
let liveChart = null

let stompClient = null
let refreshTimer = null

const timeData = ref([])
const flowData = ref([])
const speedData = ref([])

function severityTag(s) {
  if (s === 'high' || s === 'critical') return 'danger'
  if (s === 'medium') return 'warning'
  return 'info'
}

function formatTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString('zh-CN')
}

function occColor(val) {
  if (!val) return '#67c23a'
  if (val > 0.8) return '#f56c6c'
  if (val > 0.5) return '#e6a23c'
  return '#67c23a'
}

async function refreshAll() {
  try {
    const list = await webApi.listIntersections() || []
    const statuses = []
    for (const item of list) {
      try {
        const dash = await webApi.getDashboard(item.intersectionId)
        statuses.push({
          intersectionId: item.intersectionId,
          status: item.status,
          flow: dash?.trafficFeature?.flow || 0,
          speed: dash?.trafficFeature?.speed || 0,
          occupancy: dash?.trafficFeature?.occupancy || 0,
          queueLength: dash?.trafficFeature?.queueLength || 0,
          delay: dash?.trafficFeature?.delay || 0,
          lastUpdateTime: formatTime(dash?.lastUpdateTime)
        })
      } catch (e) { /* skip */ }
    }
    intersectionStatus.value = statuses
    updateChart()
  } catch (e) { console.error(e) }
}

function updateChart() {
  const now = new Date().toLocaleTimeString('zh-CN')
  timeData.value.push(now)
  if (timeData.value.length > 30) timeData.value.shift()

  const avg = intersectionStatus.value.reduce((s, r) => s + (r.flow || 0), 0)
  flowData.value.push(intersectionStatus.value.length > 0 ? avg / intersectionStatus.value.length : 0)
  if (flowData.value.length > 30) flowData.value.shift()

  speedData.value.push(intersectionStatus.value.reduce((s, r) => s + (r.speed || 0), 0) /
    Math.max(intersectionStatus.value.length, 1))
  if (speedData.value.length > 30) speedData.value.shift()

  if (!liveChart) return
  liveChart.setOption({
    xAxis: { data: [...timeData.value] },
    series: [
      { name: '平均车流量', type: 'line', smooth: true, data: [...flowData.value], itemStyle: { color: '#409eff' } },
      { name: '平均速度', type: 'line', smooth: true, data: [...speedData.value], itemStyle: { color: '#67c23a' } }
    ]
  })
}

function renderChart() {
  if (!liveChartRef.value) return
  if (!liveChart) liveChart = echarts.init(liveChartRef.value)
  liveChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['平均车流量', '平均速度'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '20%', containLabel: true },
    xAxis: { type: 'category', data: [] },
    yAxis: [{ type: 'value', name: '车流量' }, { type: 'value', name: '速度(km/h)' }],
    series: [
      { name: '平均车流量', type: 'line', smooth: true, data: [], itemStyle: { color: '#409eff' } },
      { name: '平均速度', type: 'line', smooth: true, data: [], itemStyle: { color: '#67c23a' }, yAxisIndex: 1 }
    ]
  })
}

// Auto refresh every 5 seconds
watch(autoRefresh, (val) => {
  if (val) {
    refreshTimer = setInterval(refreshAll, 5000)
  } else {
    clearInterval(refreshTimer)
  }
})

function clearAlerts() { alerts.value = [] }

onMounted(() => {
  renderChart()
  refreshAll()
  refreshTimer = setInterval(refreshAll, 5000)
})

onBeforeUnmount(() => {
  clearInterval(refreshTimer)
  liveChart?.dispose()
  stompClient?.deactivate()
})
</script>

<style scoped>
.traffic-monitor { padding: 4px; }
.section-card { border-radius: 6px; }
.alert-list { max-height: 320px; overflow-y: auto; }
.alert-item {
  display: flex; align-items: center; gap: 8px; padding: 8px;
  border-bottom: 1px solid #ebeef5; font-size: 13px;
}
.alert-item:last-child { border-bottom: none; }
.alert-item.high, .alert-item.critical { background-color: #fef0f0; }
.alert-item.medium { background-color: #fdf6ec; }
.alert-msg { flex: 1; }
.alert-time { color: #909399; font-size: 12px; white-space: nowrap; }
</style>
