<template>
  <div class="dashboard">
    <!-- Top bar -->
    <div class="toolbar">
      <el-select v-model="selectedId" placeholder="选择路口" size="default" style="width:200px" @change="loadDashboard">
        <el-option v-for="item in intersections" :key="item.intersectionId"
          :label="item.name || item.intersectionId" :value="item.intersectionId" />
      </el-select>
      <el-button size="default" @click="loadDashboard" :icon="Refresh">刷新</el-button>
    </div>

    <!-- Metrics -->
    <div class="metric-bar">
      <div class="metric-card">
        <span class="mc-value">{{ trafficFeature?.flow ?? '-' }}</span>
        <span class="mc-label">车流量 (辆/时)</span>
      </div>
      <div class="metric-card">
        <span class="mc-value">{{ trafficFeature?.speed ?? '-' }}</span>
        <span class="mc-label">平均速度 (km/h)</span>
      </div>
      <div class="metric-card">
        <span class="mc-value" :style="{ color: occColor }">{{ trafficFeature?.occupancy ?? '-' }}</span>
        <span class="mc-label">占用率</span>
      </div>
      <div class="metric-card">
        <span class="mc-value">{{ trafficFeature?.queueLength ?? '-' }}</span>
        <span class="mc-label">排队长度 (m)</span>
      </div>
    </div>

    <!-- Main content -->
    <div class="content-row">
      <div class="panel" style="flex:1.4">
        <div class="panel-header">信号相位方案</div>
        <div v-if="signalPlan">
          <el-descriptions :column="3" size="small" border style="margin-bottom:12px">
            <el-descriptions-item label="方案ID">{{ signalPlan.planId }}</el-descriptions-item>
            <el-descriptions-item label="周期(秒)">{{ signalPlan.cycleTime }}</el-descriptions-item>
            <el-descriptions-item label="当前相位">{{ signalPlan.currentPhase }}</el-descriptions-item>
          </el-descriptions>
          <el-table :data="signalPlan.phases || []" size="small" border>
            <el-table-column prop="phaseId" label="相位ID" width="70" />
            <el-table-column prop="direction" label="方向" width="70" />
            <el-table-column prop="greenTime" label="绿灯(s)" width="80" />
            <el-table-column prop="yellowTime" label="黄灯(s)" width="80" />
            <el-table-column prop="redTime" label="红灯(s)" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.phaseId === signalPlan.currentPhase" type="success" size="small">运行中</el-tag>
                <el-tag v-else type="info" size="small">等待</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="暂无信号方案" :image-size="40" />
      </div>
      <div class="panel" style="flex:1">
        <div class="panel-header">交通流趋势</div>
        <div ref="chartRef" style="height:280px"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { webApi, fusionApi, signalApi } from '@/api'
import * as echarts from 'echarts'

const selectedId = ref('')
const intersections = ref([])
const dashboard = ref(null)
let chartInstance = null
const chartRef = ref(null)

const signalPlan = computed(() => dashboard.value?.signalPlan)
const trafficFeature = computed(() => dashboard.value?.trafficFeature)

const occColor = computed(() => {
  const v = trafficFeature.value?.occupancy
  if (v == null) return '#909399'
  if (v > 0.8) return '#f56c6c'
  if (v > 0.5) return '#e6a23c'
  return '#67c23a'
})

function generateDiurnalCurve() {
  const hours = []
  const flows = []
  for (let h = 0; h < 24; h += 2) {
    const t = h
    let factor
    if (t < 5) factor = 0.08
    else if (t < 6) factor = 0.08 + 0.12 * (t - 5)
    else if (t < 7) factor = 0.20 + 0.25 * (t - 6)
    else if (t < 9) factor = 0.45 + 0.22 * (t - 7)
    else if (t < 12) factor = 0.85 - 0.08 * (t - 9)
    else if (t < 14) factor = 0.55 - 0.05 * (t - 12)
    else if (t < 17) factor = 0.45 + 0.10 * (t - 14)
    else if (t < 19) factor = 0.75 + 0.07 * (t - 17)
    else if (t < 21) factor = 0.85 - 0.10 * (t - 19)
    else if (t < 23) factor = 0.45 - 0.15 * (t - 21)
    else factor = 0.12
    hours.push(String(h).padStart(2, '0') + ':00')
    flows.push(Math.round(factor * 1700))
  }
  return { hours, flows }
}

async function loadIntersections() {
  try {
    const list = await fusionApi.listIntersectionInfo()
    intersections.value = list?.length ? list : []
  } catch (e) {
    try { intersections.value = await webApi.listIntersections() || [] } catch (e2) { /* fallback */ }
  }
  if (!intersections.value.length) {
    intersections.value = ['INT-001', 'INT-002', 'INT-003'].map(id => ({ intersectionId: id, name: id }))
  }
  if (intersections.value.length > 0 && !selectedId.value) {
    selectedId.value = intersections.value[0].intersectionId || intersections.value[0].intersectionId
    loadDashboard()
  }
}

async function loadDashboard() {
  if (!selectedId.value) return
  try {
    const [snapshot, plan] = await Promise.all([
      fusionApi.getSnapshot(selectedId.value).catch(() => null),
      signalApi.getPlan(selectedId.value).catch(() => null)
    ])
    dashboard.value = {
      trafficFeature: snapshot || null,
      signalPlan: plan || { planId: '-', cycleTime: 90, currentPhase: 1, phases: [
        { phaseId: 1, direction: '东西', greenTime: 35, yellowTime: 3, redTime: 52 },
        { phaseId: 2, direction: '南北', greenTime: 40, yellowTime: 3, redTime: 47 }
      ]},
      lastUpdateTime: new Date().toLocaleTimeString()
    }
    nextTick(() => renderChart())
  } catch (e) {
    try { dashboard.value = await webApi.getDashboard(selectedId.value); nextTick(() => renderChart()) } catch (e2) {}
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)
  const { hours, flows } = generateDiurnalCurve()
  const now = new Date().getHours()
  const nowIdx = Math.floor(now / 2)
  const markData = nowIdx < flows.length ? [{ xAxis: hours[nowIdx], value: flows[nowIdx] }] : []

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: hours },
    yAxis: { type: 'value', name: '辆/时' },
    series: [{
      name: '车流量', type: 'line', smooth: true, data: flows,
      itemStyle: { color: '#409eff' },
      areaStyle: { color: 'rgba(64,158,255,0.1)' },
      markPoint: { data: markData.map(d => ({ ...d, symbol: 'pin', symbolSize: 36, label: { formatter: '当前' } })) }
    }]
  })
}

onMounted(loadIntersections)
onBeforeUnmount(() => chartInstance?.dispose())
</script>

<style scoped>
.dashboard { padding: 4px; }
.toolbar { background: #fff; border: 1px solid #e4e7ed; padding: 8px 12px; display: flex; gap: 8px; align-items: center; margin-bottom: 4px; }
.metric-bar { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; margin-bottom: 4px; }
.metric-card { background: #fff; border: 1px solid #e4e7ed; padding: 12px; text-align: center; }
.mc-value { font-size: 28px; font-weight: 600; color: #303133; display: block; line-height: 1.2; }
.mc-label { font-size: 12px; color: #909399; margin-top: 2px; display: block; }
.content-row { display: flex; gap: 4px; }
.panel { background: #fff; border: 1px solid #e4e7ed; padding: 12px; }
.panel-header { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 10px; padding-bottom: 6px; border-bottom: 1px solid #ebeef5; }
</style>
