<template>
  <div class="dashboard">
    <!-- Intersection selector -->
    <el-card shadow="never" class="section-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-select v-model="selectedId" placeholder="选择路口" size="large" style="width:100%"
            @change="loadDashboard">
            <el-option v-for="item in intersections" :key="item.intersectionId"
              :label="item.name || item.intersectionId" :value="item.intersectionId">
              <span>{{ item.name || item.intersectionId }}</span>
              <el-tag :type="statusType(item.status)" size="small" style="float:right">
                {{ item.status }}
              </el-tag>
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" size="large" @click="loadDashboard" :icon="Refresh">刷新</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Status cards -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value" style="color:#409eff">{{ trafficFeature?.flow || '-' }}</div>
          <div class="stat-label">车流量 (辆/时)</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value" style="color:#67c23a">{{ trafficFeature?.speed || '-' }}</div>
          <div class="stat-label">平均速度 (km/h)</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value" :style="{ color: occupancyColor }">{{ trafficFeature?.occupancy || '-' }}</div>
          <div class="stat-label">占用率</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value" style="color:#e6a23c">{{ trafficFeature?.queueLength || '-' }}</div>
          <div class="stat-label">排队长度 (m)</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Main content -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card shadow="never" class="section-card">
          <template #header><span style="font-weight:600">信号相位方案</span></template>
          <div v-if="signalPlan">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="方案ID">{{ signalPlan.planId }}</el-descriptions-item>
              <el-descriptions-item label="周期(秒)">{{ signalPlan.cycleTime }}</el-descriptions-item>
              <el-descriptions-item label="当前相位">{{ signalPlan.currentPhase }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ dashboard?.lastUpdateTime || '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-divider />
            <el-table :data="signalPlan.phases || []" size="small" border stripe>
              <el-table-column prop="phaseId" label="相位ID" width="80" />
              <el-table-column prop="direction" label="方向" width="80" />
              <el-table-column prop="greenTime" label="绿灯(s)" width="90" />
              <el-table-column prop="yellowTime" label="黄灯(s)" width="90" />
              <el-table-column prop="redTime" label="红灯(s)" width="90" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag v-if="row.phaseId === signalPlan.currentPhase" type="success" size="small">运行中</el-tag>
                  <el-tag v-else type="info" size="small">等待</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="暂无信号方案" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="section-card">
          <template #header><span style="font-weight:600">交通流趋势</span></template>
          <div ref="chartRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Active routes -->
    <el-card shadow="never" class="section-card" style="margin-top:16px">
      <template #header><span style="font-weight:600">活跃路径</span></template>
      <el-table :data="dashboard?.activeRoutes || []" size="small" border stripe v-if="dashboard?.activeRoutes?.length">
        <el-table-column prop="routeId" label="路径ID" />
        <el-table-column prop="originId" label="起点" />
        <el-table-column prop="destinationId" label="终点" />
        <el-table-column prop="estimatedTime" label="预计时间(min)" />
        <el-table-column prop="totalDistance" label="距离(km)" />
      </el-table>
      <el-empty v-else description="暂无活跃路径" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { webApi } from '@/api'
import * as echarts from 'echarts'

const selectedId = ref('')
const intersections = ref([])
const dashboard = ref(null)
let chartInstance = null
const chartRef = ref(null)

const signalPlan = computed(() => dashboard.value?.signalPlan)
const trafficFeature = computed(() => dashboard.value?.trafficFeature)

const occupancyColor = computed(() => {
  const v = trafficFeature.value?.occupancy
  if (v == null) return '#909399'
  if (v > 0.8) return '#f56c6c'
  if (v > 0.5) return '#e6a23c'
  return '#67c23a'
})

function statusType(status) {
  if (status === '正常' || status === '畅通') return 'success'
  if (status === '拥堵') return 'danger'
  if (status === '缓行') return 'warning'
  return 'info'
}

async function loadIntersections() {
  try {
    intersections.value = await webApi.listIntersections() || []
    if (intersections.value.length > 0 && !selectedId.value) {
      selectedId.value = intersections.value[0].intersectionId
      loadDashboard()
    }
  } catch (e) { console.error('Failed to load intersections:', e) }
}

async function loadDashboard() {
  if (!selectedId.value) return
  try {
    dashboard.value = await webApi.getDashboard(selectedId.value)
    nextTick(() => renderChart())
  } catch (e) { console.error('Failed to load dashboard:', e) }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00'] },
    yAxis: { type: 'value' },
    series: [
      {
        name: '车流量', type: 'line', smooth: true, data: [120, 80, 450, 380, 520, 300],
        itemStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64,158,255,0.1)' }
      }
    ]
  })
}

onMounted(loadIntersections)
onBeforeUnmount(() => chartInstance?.dispose())
</script>

<style scoped>
.dashboard { padding: 4px; }
.stat-card { text-align: center; }
.stat-value { font-size: 32px; font-weight: bold; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.section-card { border-radius: 6px; }
</style>
