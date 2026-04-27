<template>
  <div class="traffic-monitor">
    <!-- Control bar -->
    <el-card shadow="never" class="control-bar">
      <el-row :gutter="12" align="middle">
        <el-col :span="6">
          <el-select v-model="selectedId" placeholder="选择路口" size="large" style="width:100%"
            @change="loadData">
            <el-option v-for="item in intersections" :key="item.intersectionId"
              :label="item.name || item.intersectionId" :value="item.intersectionId" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" size="large" @click="toggleSimulation" :icon="simRunning ? VideoPause : VideoPlay" style="width:100%">
            {{ simRunning ? '暂停' : '运行' }}
          </el-button>
        </el-col>
        <el-col :span="8">
          <div class="status-bar">
            <el-tag :type="simRunning ? 'success' : 'info'" effect="dark" size="default">
              {{ simRunning ? '仿真运行中' : '已暂停' }}
            </el-tag>
            <el-tag type="warning" effect="plain" size="default">
              车流量: {{ trafficFlow }}
            </el-tag>
            <el-tag :type="congestionTag" effect="plain" size="default">
              {{ congestionText }}
            </el-tag>
          </div>
        </el-col>
        <el-col :span="6" style="text-align:right">
          <el-button-group>
            <el-button :type="viewMode === '3d' ? 'primary' : 'default'" @click="viewMode = '3d'" size="default">3D</el-button>
            <el-button :type="viewMode === 'data' ? 'primary' : 'default'" @click="viewMode = 'data'" size="default">数据</el-button>
          </el-button-group>
        </el-col>
      </el-row>
    </el-card>

    <!-- 3D View -->
    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="viewMode === '3d' ? 16 : 24">
        <el-card shadow="never" class="scene-card" :style="{ height: viewMode === '3d' ? '520px' : 'auto' }">
          <Intersection3D
            v-if="show3d"
            :intersection-id="selectedId"
            :phases="phases"
            :current-phase-id="currentPhaseId"
            :flow="trafficFlow"
            :speed="trafficSpeed"
            :occupancy="trafficOccupancy"
            @adjust-phase="onAdjustPhase"
          />
          <div v-else style="height:500px;display:flex;align-items:center;justify-content:center;color:#909399">
            选择路口查看3D场景
          </div>
        </el-card>
      </el-col>

      <!-- Side panel (3D mode) -->
      <el-col :span="8" v-if="viewMode === '3d'">
        <el-card shadow="never" class="info-card">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">{{ selectedId }} 实时数据</span>
              <el-tag size="small" :type="online ? 'success' : 'danger'" effect="dark">
                {{ online ? '在线' : '离线' }}
              </el-tag>
            </div>
          </template>

          <div class="metric-grid">
            <div class="metric-item">
              <div class="metric-icon" style="background:#409eff20;color:#409eff">🚗</div>
              <div class="metric-info">
                <span class="metric-val">{{ Math.round(trafficFlow) }}</span>
                <span class="metric-lbl">车流量(辆/时)</span>
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-icon" style="background:#67c23a20;color:#67c23a">⚡</div>
              <div class="metric-info">
                <span class="metric-val">{{ trafficSpeed.toFixed(1) }}</span>
                <span class="metric-lbl">速度(km/h)</span>
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-icon" :style="{ background: occColor + '20', color: occColor }">📊</div>
              <div class="metric-info">
                <span class="metric-val">{{ (trafficOccupancy * 100).toFixed(0) }}%</span>
                <span class="metric-lbl">占用率</span>
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-icon" style="background:#e6a23c20;color:#e6a23c">📏</div>
              <div class="metric-info">
                <span class="metric-val">{{ queueLength.toFixed(0) }}</span>
                <span class="metric-lbl">排队长度(m)</span>
              </div>
            </div>
          </div>

          <el-divider style="margin:12px 0" />

          <div style="font-weight:600;margin-bottom:8px;font-size:13px">信号相位</div>
          <div v-for="ph in phases" :key="ph.phaseId" class="phase-row"
            :class="{ active: String(ph.phaseId) === String(currentPhaseId) }"
            @click="switchPhase(ph.phaseId)">
            <div class="phase-dir">{{ ph.direction }}</div>
            <div class="phase-lights">
              <span class="phase-light red" :class="{ on: ph.phaseId === currentPhaseId && ph.greenTime > 0 }"></span>
              <span class="phase-light yellow" :class="{ on: false }"></span>
              <span class="phase-light green" :class="{ on: String(ph.phaseId) === String(currentPhaseId) }" :style="{ width: (ph.greenTime / 90 * 60) + 'px' }"></span>
              <span class="phase-time">{{ ph.greenTime }}s</span>
            </div>
            <el-button text type="primary" size="small" @click.stop="showPhaseDialog(ph)">调整</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Data view (detailed tables) -->
    <el-row :gutter="12" style="margin-top:12px" v-if="viewMode === 'data'">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header><span style="font-weight:600">路口状态列表</span></template>
          <el-table :data="allStatus" border stripe size="small" v-if="allStatus.length">
            <el-table-column prop="intersectionId" label="路口" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === '拥堵' ? 'danger' : row.status === '缓行' ? 'warning' : 'success'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="flow" label="车流量" width="100" />
            <el-table-column prop="speed" label="速度" width="80" />
            <el-table-column label="占用率" width="120">
              <template #default="{ row }">
                <el-progress :percentage="Math.round((row.occupancy || 0) * 100)" :color="occColor" :stroke-width="10" />
              </template>
            </el-table-column>
            <el-table-column prop="queueLength" label="排队长度" width="90" />
            <el-table-column prop="delay" label="延误" width="70" />
            <el-table-column prop="lastUpdate" label="更新时间" />
          </el-table>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Adjust dialog -->
    <el-dialog v-model="dialogVisible" title="调整绿灯时间" width="400px">
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="方向">{{ adjustForm.direction }}</el-form-item>
        <el-form-item label="绿灯时间">
          <el-slider v-model="adjustForm.greenTime" :min="10" :max="120" :step="5" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust">应用</el-button>
      </template>
    </el-dialog>

    <!-- Alert panel -->
    <el-card v-if="alerts.length" shadow="never" class="alert-panel">
      <template #header>
        <span style="font-weight:600;color:#f56c6c">🚨 实时告警</span>
        <el-button size="small" text @click="alerts = []">清空</el-button>
      </template>
      <div class="alert-scroll">
        <div v-for="(a, i) in alerts" :key="i" class="alert-row" :class="a.severity">
          <el-tag :type="a.severity === 'high' ? 'danger' : 'warning'" size="small">{{ a.alertType }}</el-tag>
          <span class="alert-msg">{{ a.message }}</span>
          <span class="alert-time">{{ a.time }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { webApi, signalApi } from '@/api'
import { ElMessage } from 'element-plus'
import Intersection3D from '@/components/Intersection3D.vue'

const selectedId = ref('INT-001')
const intersections = ref([])
const viewMode = ref('3d')
const simRunning = ref(true)
const online = ref(false)
const show3d = ref(false)

// Demo data
const phases = ref([
  { phaseId: 1, direction: '东西', greenTime: 35, yellowTime: 3, redTime: 52 },
  { phaseId: 2, direction: '南北', greenTime: 40, yellowTime: 3, redTime: 47 }
])
const currentPhaseId = ref(1)
const trafficFlow = ref(520)
const trafficSpeed = ref(38)
const trafficOccupancy = ref(0.45)
const queueLength = ref(28)
const allStatus = ref([])
const alerts = ref([])

let simTimer = null
let alertTimer = null

const congestionTag = computed(() => {
  if (trafficOccupancy.value > 0.8) return 'danger'
  if (trafficOccupancy.value > 0.5) return 'warning'
  return 'success'
})
const congestionText = computed(() => {
  if (trafficOccupancy.value > 0.8) return '严重拥堵'
  if (trafficOccupancy.value > 0.6) return '拥堵'
  if (trafficOccupancy.value > 0.4) return '缓行'
  return '畅通'
})
const occColor = computed(() => {
  if (trafficOccupancy.value > 0.8) return '#f56c6c'
  if (trafficOccupancy.value > 0.5) return '#e6a23c'
  return '#67c23a'
})

// Phase adjust dialog
const dialogVisible = ref(false)
const adjustForm = ref({ phaseId: null, direction: '', greenTime: 30 })

function showPhaseDialog(ph) {
  adjustForm.value = { phaseId: ph.phaseId, direction: ph.direction, greenTime: ph.greenTime }
  dialogVisible.value = true
}

function confirmAdjust() {
  const ph = phases.value.find(p => p.phaseId === adjustForm.value.phaseId)
  if (ph) {
    ph.greenTime = adjustForm.value.greenTime
    // Recalc cycle
    const totalYellow = phases.value.reduce((s, p) => s + p.yellowTime, 0)
    const totalRed = phases.value.reduce((s, p) => s + p.redTime, 0)
    const totalGreen = phases.value.reduce((s, p) => s + p.greenTime, 0)
    ElMessage.success(`相位 ${ph.direction} 绿灯时间已调整为 ${ph.greenTime}s，周期 ${totalGreen + totalYellow + totalRed}s`)
  }
  dialogVisible.value = false
}

function switchPhase(phaseId) {
  currentPhaseId.value = phaseId
}

function onAdjustPhase({ direction, type }) {
  const dirMap = { north: '南北', south: '南北', east: '东西', west: '东西' }
  const dir = dirMap[direction]
  const ph = phases.value.find(p => p.direction === dir)
  if (ph) {
    showPhaseDialog(ph)
  }
}

function toggleSimulation() {
  simRunning.value = !simRunning.value
}

function runSimulation() {
  if (!simRunning.value) return
  // Vary traffic data randomly for visual effect
  trafficFlow.value = Math.max(100, trafficFlow.value + (Math.random() - 0.5) * 60)
  trafficSpeed.value = Math.max(5, Math.min(80, trafficSpeed.value + (Math.random() - 0.5) * 5))
  trafficOccupancy.value = Math.max(0.1, Math.min(1, trafficOccupancy.value + (Math.random() - 0.5) * 0.08))
  queueLength.value = Math.max(0, queueLength.value + (Math.random() - 0.5) * 10)

  // Random alert generation
  if (trafficOccupancy.value > 0.85 && Math.random() > 0.7) {
    alerts.value.unshift({
      alertType: '拥堵告警',
      message: `${selectedId.value} 占用率 ${(trafficOccupancy.value * 100).toFixed(0)}%`,
      severity: 'high',
      time: new Date().toLocaleTimeString()
    })
    if (alerts.value.length > 20) alerts.value = alerts.value.slice(0, 20)
  }

  // Switch phase periodically
  if (Math.random() > 0.95) {
    currentPhaseId.value = currentPhaseId.value === 1 ? 2 : 1
  }
}

async function loadData() {
  if (!selectedId.value) return
  try {
    const plan = await signalApi.getPlan(selectedId.value)
    if (plan?.phases?.length) {
      phases.value = plan.phases
      currentPhaseId.value = plan.currentPhase || phases.value[0]?.phaseId
    }
    online.value = true
  } catch (e) {
    // Use demo data
    online.value = false
  }
  show3d.value = true
}

async function loadIntersections() {
  try {
    intersections.value = await webApi.listIntersections() || []
    if (!intersections.value.length) {
      intersections.value = ['INT-001', 'INT-002', 'INT-003', 'INT-004'].map(id => ({ intersectionId: id, name: id }))
    }
  } catch (e) {
    intersections.value = ['INT-001', 'INT-002', 'INT-003', 'INT-004'].map(id => ({ intersectionId: id, name: id }))
  }
}

onMounted(() => {
  loadIntersections()
  nextTick(() => loadData())
  simTimer = setInterval(runSimulation, 1500)
})

onBeforeUnmount(() => {
  clearInterval(simTimer)
  clearInterval(alertTimer)
})
</script>

<style scoped>
.traffic-monitor { padding: 4px; }
.control-bar { border-radius: 8px; }
.status-bar { display: flex; gap: 8px; align-items: center; }
.scene-card { border-radius: 8px; overflow: hidden; }
.info-card { border-radius: 8px; }
.info-card .el-card__body { padding: 16px; }

.metric-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.metric-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px; border-radius: 8px; background: #f8f9fc;
}
.metric-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
.metric-info { display: flex; flex-direction: column; }
.metric-val { font-size: 20px; font-weight: bold; line-height: 1.2; }
.metric-lbl { font-size: 11px; color: #909399; }

.phase-row {
  display: flex; align-items: center; gap: 8px; padding: 8px;
  border-radius: 6px; cursor: pointer; transition: all 0.2s;
  border: 1px solid transparent; margin-bottom: 4px;
}
.phase-row:hover { background: #f0f5ff; border-color: #d9e6ff; }
.phase-row.active { background: #ecf5ff; border-color: #409eff; }
.phase-dir { font-weight: 600; width: 40px; font-size: 13px; }
.phase-lights { flex: 1; display: flex; align-items: center; gap: 4px; }
.phase-light {
  height: 16px; border-radius: 3px; transition: all 0.3s;
}
.phase-light.red { width: 50px; background: #f0f0f0; }
.phase-light.red.on { background: #f56c6c; box-shadow: 0 0 6px #f56c6c; }
.phase-light.yellow { width: 16px; background: #f0f0f0; }
.phase-light.yellow.on { background: #e6a23c; box-shadow: 0 0 6px #e6a23c; }
.phase-light.green { width: 30px; background: #f0f0f0; }
.phase-light.green.on { background: #67c23a; box-shadow: 0 0 6px #67c23a; }
.phase-time { font-size: 12px; color: #909399; margin-left: auto; }

.alert-panel { margin-top: 12px; border-radius: 8px; }
.alert-scroll { max-height: 120px; overflow-y: auto; }
.alert-row {
  display: flex; align-items: center; gap: 8px; padding: 6px 8px;
  font-size: 13px; border-bottom: 1px solid #f0f0f0;
}
.alert-row.high { background: #fef0f0; border-radius: 4px; }
.alert-msg { flex: 1; }
.alert-time { color: #909399; font-size: 12px; white-space: nowrap; }
</style>
