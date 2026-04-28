<template>
  <div class="traffic-monitor">
    <!-- Control bar -->
    <div class="control-bar">
      <div class="control-row">
        <el-select v-model="selectedId" placeholder="选择路口" size="default" style="width:200px"
          @change="onIntersectionChange">
          <el-option v-for="item in intersections" :key="item.intersectionId"
            :label="item.name || item.intersectionId" :value="item.intersectionId" />
        </el-select>
        <el-button type="primary" @click="toggleSimulation" size="default">
          {{ simRunning ? '暂停' : '运行' }}
        </el-button>
        <div class="status-tags">
          <el-tag :type="backendOnline ? 'success' : 'warning'" size="small" effect="plain">
            {{ backendOnline ? 'API' : '本地' }}
          </el-tag>
          <el-tag :type="dataStatus.type" size="small" effect="plain">
            {{ dataStatus.text }}
          </el-tag>
          <span class="status-time">{{ currentTime }}</span>
        </div>
        <div style="margin-left:auto">
          <el-button-group>
            <el-button :type="viewMode === '3d' ? 'primary' : 'default'" size="small" @click="viewMode = '3d'">3D</el-button>
            <el-button :type="viewMode === 'data' ? 'primary' : 'default'" size="small" @click="viewMode = 'data'">列表</el-button>
          </el-button-group>
        </div>
      </div>
    </div>

    <!-- 3D View -->
    <div class="content-row">
      <div class="scene-area" :class="{ full: viewMode !== '3d' }" v-if="viewMode === '3d'">
        <div class="scene-panel">
          <Intersection3D
            v-if="show3d"
            :key="selectedId"
            :intersection-id="selectedId"
            :phases="phases"
            :current-phase-id="currentPhaseId"
            :flow="trafficFlow"
            :speed="trafficSpeed"
            :occupancy="trafficOccupancy"
            :approaches="currentApproaches"
            @adjust-phase="onAdjustPhase"
          />
          <div v-else class="scene-placeholder">
            <span>选择路口查看3D场景</span>
          </div>
        </div>
      </div>

      <!-- Metrics panel -->
      <div class="metrics-panel" v-if="viewMode === '3d'">
        <div class="panel-block">
          <div class="panel-block-title">{{ selectedLabel }}</div>
          <div class="metrics-grid">
            <div class="metric">
              <span class="metric-value">{{ Math.round(trafficFlow) }}</span>
              <span class="metric-label">车流量 (辆/时)</span>
            </div>
            <div class="metric">
              <span class="metric-value">{{ trafficSpeed.toFixed(1) }}</span>
              <span class="metric-label">速度 (km/h)</span>
            </div>
            <div class="metric">
              <span class="metric-value">{{ (trafficOccupancy * 100).toFixed(0) }}%</span>
              <span class="metric-label">占用率</span>
            </div>
            <div class="metric">
              <span class="metric-value">{{ queueLength.toFixed(0) }}</span>
              <span class="metric-label">排队长度 (m)</span>
            </div>
          </div>
        </div>

        <div class="panel-block">
          <div class="panel-block-title">信号相位</div>
          <div v-for="ph in phases" :key="ph.phaseId" class="phase-item"
            :class="{ active: String(ph.phaseId) === String(currentPhaseId) }"
            @click="switchPhase(ph.phaseId)">
            <div class="phase-direction">{{ ph.direction }}</div>
            <div class="phase-times">
              <span class="phase-seg green" :class="{ on: String(ph.phaseId) === String(currentPhaseId) }" :style="{ width: greenPct(ph) + '%' }">{{ ph.greenTime }}s</span>
              <span class="phase-seg yellow" :style="{ width: yellowPct(ph) + '%' }">{{ ph.yellowTime }}s</span>
              <span class="phase-seg red" :class="{ on: String(ph.phaseId) !== String(currentPhaseId) }" :style="{ width: redPct(ph) + '%' }">{{ ph.redTime }}s</span>
            </div>
            <el-button text type="primary" size="small" @click.stop="showPhaseDialog(ph)">调整</el-button>
          </div>
        </div>

        <div class="panel-block">
          <div class="info-line"><span>延误</span><span>{{ delay.toFixed(1) }}s</span></div>
          <div class="info-line"><span>通行能力</span><span>{{ currentCapacity || '-' }}</span></div>
          <div class="info-line"><span>位置</span><span>{{ currentGps || '-' }}</span></div>
        </div>
      </div>

      <!-- Full-width data table -->
      <div class="scene-area full" v-if="viewMode === 'data'">
        <div class="table-panel">
          <div class="table-toolbar">
            <span class="table-title">路口状态 ({{ allStatus.length }})</span>
            <el-button size="small" @click="refreshAllData" :loading="loading">刷新</el-button>
          </div>
          <el-table :data="allStatus" border size="small" v-if="allStatus.length" max-height="480" style="width:100%">
            <el-table-column prop="intersectionId" label="路口" width="90" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <span :style="{ color: occColorFromVal(row.occupancy) }">{{ row.congestionLevel }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="flow" label="车流量" width="90">
              <template #default="{ row }">{{ Math.round(row.flow) }}</template>
            </el-table-column>
            <el-table-column prop="speed" label="速度" width="80">
              <template #default="{ row }">{{ row.speed.toFixed(1) }}</template>
            </el-table-column>
            <el-table-column label="占用率" width="120">
              <template #default="{ row }">
                <el-progress :percentage="Math.round((row.occupancy || 0) * 100)" :color="occColorFromVal(row.occupancy)" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column prop="queueLength" label="排队" width="70">
              <template #default="{ row }">{{ row.queueLength.toFixed(0) }}m</template>
            </el-table-column>
            <el-table-column prop="delay" label="延误" width="60">
              <template #default="{ row }">{{ row.delay.toFixed(1) }}s</template>
            </el-table-column>
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ formatTime(row.timestamp) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="60" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="selectedId = row.intersectionId; viewMode='3d'">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="正在加载数据..." />
        </div>
      </div>
    </div>

    <!-- Phase adjust dialog -->
    <el-dialog v-model="dialogVisible" title="调整绿灯时间" width="380px">
      <el-form :model="adjustForm" label-width="80px">
        <el-form-item label="方向">{{ adjustForm.direction }}</el-form-item>
        <el-form-item label="绿灯时间">
          <el-slider v-model="adjustForm.greenTime" :min="10" :max="120" :step="5" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="confirmAdjust">应用</el-button>
      </template>
    </el-dialog>

    <!-- Alerts -->
    <div v-if="alerts.length" class="alert-bar">
      <span class="alert-title">告警 ({{ alerts.length }})</span>
      <el-button size="small" text @click="alerts = []">清空</el-button>
      <div class="alert-list">
        <div v-for="(a, i) in alerts" :key="i" class="alert-item" :class="a.severity">
          <el-tag :type="a.severity === 'high' ? 'danger' : 'warning'" size="small">{{ a.type }}</el-tag>
          <span class="alert-message">{{ a.message }}</span>
          <span class="alert-time">{{ a.time }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { fusionApi, signalApi } from '@/api'
import { ElMessage } from 'element-plus'
import Intersection3D from '@/components/Intersection3D.vue'

const selectedId = ref('INT-001')
const intersections = ref([])
const intersectionMeta = ref({})
const viewMode = ref('3d')
const simRunning = ref(true)
const backendOnline = ref(false)
const show3d = ref(false)
const loading = ref(false)

const phases = ref([
  { phaseId: 1, direction: '东西', greenTime: 35, yellowTime: 3, redTime: 52 },
  { phaseId: 2, direction: '南北', greenTime: 40, yellowTime: 3, redTime: 47 }
])
const currentPhaseId = ref(1)
const trafficFlow = ref(0)
const trafficSpeed = ref(0)
const trafficOccupancy = ref(0)
const queueLength = ref(0)
const delay = ref(0)
const congestionLevel = ref('')
const allStatus = ref([])
const alerts = ref([])

let pollTimer = null
const currentTime = ref(new Date().toLocaleTimeString())

const selectedLabel = computed(() => intersectionMeta.value[selectedId.value]?.name || selectedId.value)
const currentApproaches = computed(() => intersectionMeta.value[selectedId.value]?.approaches || null)
const currentCapacity = computed(() => intersectionMeta.value[selectedId.value]?.capacity || null)
const currentGps = computed(() => {
  const m = intersectionMeta.value[selectedId.value]
  return m ? `${m.latitude.toFixed(4)}, ${m.longitude.toFixed(4)}` : null
})

const dataStatus = computed(() => {
  const occ = trafficOccupancy.value
  if (occ > 0.8) return { type: 'danger', text: '严重拥堵' }
  if (occ > 0.6) return { type: 'warning', text: '拥堵' }
  if (occ > 0.4) return { type: 'warning', text: '缓行' }
  return { type: 'success', text: '畅通' }
})

function greenPct(p) { const t = p.greenTime + p.yellowTime + p.redTime; return t > 0 ? (p.greenTime / t) * 100 : 33 }
function yellowPct(p) { const t = p.greenTime + p.yellowTime + p.redTime; return t > 0 ? (p.yellowTime / t) * 100 : 33 }
function redPct(p) { const t = p.greenTime + p.yellowTime + p.redTime; return t > 0 ? (p.redTime / t) * 100 : 33 }
function occColorFromVal(occ) {
  if (occ > 0.8) return '#f56c6c'
  if (occ > 0.5) return '#e6a23c'
  return '#67c23a'
}
function formatTime(ts) { return ts ? new Date(ts).toLocaleTimeString() : '-' }

const dialogVisible = ref(false)
const adjustForm = ref({ phaseId: null, direction: '', greenTime: 30 })

function showPhaseDialog(ph) {
  adjustForm.value = { phaseId: ph.phaseId, direction: ph.direction, greenTime: ph.greenTime }
  dialogVisible.value = true
}

async function confirmAdjust() {
  const ph = phases.value.find(p => p.phaseId === adjustForm.value.phaseId)
  if (ph) {
    ph.greenTime = adjustForm.value.greenTime
    try {
      await signalApi.adjustPhase({
        planId: selectedId.value,
        phaseId: ph.phaseId,
        intersectionId: selectedId.value,
        suggestedGreenTime: adjustForm.value.greenTime,
        flow: trafficFlow.value,
        speed: trafficSpeed.value,
        occupancy: trafficOccupancy.value,
        queueLength: queueLength.value,
        delay: delay.value
      })
      ElMessage.success(`相位 ${ph.direction} 绿灯已调整为 ${ph.greenTime}s，已提交DRL优化`)
    } catch (e) {
      ElMessage.warning(`本地已更新，但DRL引擎未响应: ${e.message}`)
    }
  }
  dialogVisible.value = false
}

function switchPhase(phaseId) { currentPhaseId.value = phaseId }

function onAdjustPhase({ direction, type }) {
  const dirMap = { north: '南北', south: '南北', east: '东西', west: '东西' }
  const ph = phases.value.find(p => p.direction === dirMap[direction])
  if (ph) showPhaseDialog(ph)
}

function toggleSimulation() { simRunning.value = !simRunning.value }

async function fetchSimulationData() {
  if (!simRunning.value) return
  try {
    const snapshots = await fusionApi.getAllSnapshots()
    if (snapshots?.length) {
      backendOnline.value = true
      const current = snapshots.find(s => s.intersectionId === selectedId.value)
      if (current) applySnapshot(current)
      allStatus.value = snapshots
      for (const s of snapshots) {
        if (s.occupancy > 0.85 && Math.random() > 0.7) {
          alerts.value.unshift({
            type: '拥堵', message: `${s.intersectionId} 占用率 ${(s.occupancy * 100).toFixed(0)}%`,
            severity: 'high', time: new Date().toLocaleTimeString()
          })
        }
      }
      if (alerts.value.length > 20) alerts.value = alerts.value.slice(0, 20)
    }
  } catch (e) {
    backendOnline.value = false
    runLocalSimulation()
  }
}

function runLocalSimulation() {
  const hour = new Date().getHours()
  const baseFactor = hour >= 7 && hour <= 9 ? 0.8 : hour >= 17 && hour <= 19 ? 0.8 : hour >= 22 || hour <= 5 ? 0.15 : 0.5
  const factor = Math.max(0.05, Math.min(0.95, baseFactor + (Math.random() - 0.5) * 0.15))
  const cap = 1700
  trafficFlow.value = Math.round(factor * cap)
  trafficSpeed.value = Math.round((factor < 0.3 ? 55 + Math.random() * 10 : factor < 0.6 ? 50 * (1 - 0.3 * (factor - 0.3) / 0.3) : 35 * (0.7 - 0.4 * (factor - 0.6) / 0.25)) * 10) / 10
  trafficOccupancy.value = Math.round(Math.min(1, (factor < 0.1 ? factor * 2.5 : factor < 0.5 ? 0.25 + (factor - 0.1) * 0.75 : 0.55 + (factor - 0.5) * 1.0)) * 1000) / 1000
  queueLength.value = Math.round((trafficOccupancy.value < 0.3 ? trafficOccupancy.value * 60 : 18 + (trafficOccupancy.value - 0.3) * 120) * 10) / 10
  delay.value = Math.round((trafficOccupancy.value < 0.3 ? trafficOccupancy.value * 20 : 6 + (trafficOccupancy.value - 0.3) * 40) * 10) / 10
  allStatus.value = [{ intersectionId: selectedId.value, flow: trafficFlow.value, speed: trafficSpeed.value, occupancy: trafficOccupancy.value, queueLength: queueLength.value, delay: delay.value, timestamp: Date.now() }]
}

function applySnapshot(snap) {
  trafficFlow.value = snap.flow; trafficSpeed.value = snap.speed
  trafficOccupancy.value = snap.occupancy; queueLength.value = snap.queueLength
  delay.value = snap.delay; congestionLevel.value = snap.congestionLevel
}

async function loadIntersectionMeta() {
  try {
    const list = await fusionApi.listIntersectionInfo()
    if (list?.length) {
      intersections.value = list
      const metaMap = {}
      for (const item of list) metaMap[item.intersectionId] = item
      intersectionMeta.value = metaMap
      return
    }
  } catch (e) { /* fallback */ }
  intersections.value = ['INT-001', 'INT-002', 'INT-003', 'INT-004', 'INT-005', 'INT-006', 'INT-007', 'INT-008', 'INT-009'].map(id => ({ intersectionId: id, name: id }))
}

async function loadSignalPlan() {
  try {
    const plan = await signalApi.getPlan(selectedId.value)
    if (plan?.phases?.length) { phases.value = plan.phases; currentPhaseId.value = plan.currentPhase || phases.value[0]?.phaseId }
  } catch (e) { /* use defaults */ }
}

async function onIntersectionChange() {
  show3d.value = false
  await nextTick()
  show3d.value = true
  loadSignalPlan()
  fetchSimulationData()
}

async function refreshAllData() {
  loading.value = true
  await fetchSimulationData()
  loading.value = false
}

onMounted(async () => {
  await loadIntersectionMeta()
  if (intersections.value.length > 0) selectedId.value = intersections.value[0].intersectionId
  await nextTick()
  show3d.value = true
  loadSignalPlan()
  pollTimer = setInterval(fetchSimulationData, 2000)
  fetchSimulationData()
  setInterval(() => { currentTime.value = new Date().toLocaleTimeString() }, 1000)
})

onBeforeUnmount(() => { clearInterval(pollTimer) })
</script>

<style scoped>
.traffic-monitor { padding: 4px; height: calc(100vh - 100px); display: flex; flex-direction: column; gap: 4px; }
.control-bar {
  background: #fff; border: 1px solid #e4e7ed; padding: 8px 12px; flex-shrink: 0;
}
.control-row { display: flex; align-items: center; gap: 8px; }
.status-tags { display: flex; align-items: center; gap: 6px; }
.status-time { font-size: 12px; color: #909399; }

.content-row { flex: 1; display: flex; gap: 4px; min-height: 0; }
.scene-area { flex: 1; display: flex; }
.scene-area.full { flex: 1; }
.scene-panel { flex: 1; background: #fff; border: 1px solid #e4e7ed; overflow: hidden; }
.scene-placeholder { height: 100%; display: flex; align-items: center; justify-content: center; color: #909399; font-size: 14px; }

.metrics-panel { width: 280px; min-width: 280px; background: #fff; border: 1px solid #e4e7ed; overflow-y: auto; display: flex; flex-direction: column; gap: 1px; }
.panel-block { padding: 12px; border-bottom: 1px solid #f0f0f0; }
.panel-block-title { font-size: 12px; font-weight: 600; color: #303133; margin-bottom: 8px; padding-bottom: 6px; border-bottom: 1px solid #ebeef5; }

.metrics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.metric { padding: 8px; background: #fafafa; border-radius: 3px; }
.metric-value { display: block; font-size: 20px; font-weight: 600; color: #303133; line-height: 1.2; }
.metric-label { display: block; font-size: 11px; color: #909399; margin-top: 2px; }

.phase-item { display: flex; align-items: center; gap: 6px; padding: 6px 4px; border-radius: 3px; cursor: pointer; border: 1px solid transparent; margin-bottom: 4px; }
.phase-item:hover { background: #f5f7fa; }
.phase-item.active { border-color: #409eff; background: #ecf5ff; }
.phase-direction { width: 36px; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.phase-times { flex: 1; display: flex; height: 18px; border-radius: 3px; overflow: hidden; }
.phase-seg { display: flex; align-items: center; justify-content: center; font-size: 10px; color: #fff; transition: width 0.3s; }
.phase-seg.green { background: #e8f5e9; color: #67c23a; }
.phase-seg.green.on { background: #67c23a; color: #fff; }
.phase-seg.yellow { background: #fff3e0; color: #e6a23c; }
.phase-seg.red { background: #ffebee; color: #f56c6c; }
.phase-seg.red.on { background: #f56c6c; color: #fff; }

.info-line { display: flex; justify-content: space-between; font-size: 12px; color: #606266; padding: 3px 0; border-bottom: 1px solid #f5f5f5; }

.table-panel { flex: 1; background: #fff; border: 1px solid #e4e7ed; padding: 12px; }
.table-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.table-title { font-size: 13px; font-weight: 600; color: #303133; }

.alert-bar { background: #fff; border: 1px solid #e4e7ed; padding: 8px 12px; display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.alert-title { font-size: 12px; font-weight: 600; color: #f56c6c; white-space: nowrap; }
.alert-list { display: flex; gap: 8px; overflow-x: auto; flex: 1; }
.alert-item { display: flex; align-items: center; gap: 6px; font-size: 12px; white-space: nowrap; padding: 2px 6px; border-radius: 3px; }
.alert-item.high { background: #fef0f0; }
.alert-message { color: #606266; }
.alert-time { color: #909399; font-size: 11px; }
</style>
