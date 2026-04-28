<template>
  <div class="signal-control">
    <div class="toolbar">
      <el-select v-model="selectedId" placeholder="选择路口" size="default" style="width:200px" @change="loadPlan">
        <el-option v-for="item in intersections" :key="item.intersectionId"
          :label="item.name || item.intersectionId" :value="item.intersectionId" />
      </el-select>
      <el-button size="default" @click="loadPlan">刷新</el-button>
    </div>

    <div class="content-row">
      <div class="panel" style="flex:1">
        <div class="panel-header">信号相位详情</div>
        <div v-if="plan">
          <el-descriptions :column="3" size="small" border style="margin-bottom:12px">
            <el-descriptions-item label="方案ID">{{ plan.planId }}</el-descriptions-item>
            <el-descriptions-item label="周期">{{ plan.cycleTime }}s</el-descriptions-item>
            <el-descriptions-item label="当前相位">{{ plan.currentPhase }}</el-descriptions-item>
          </el-descriptions>
          <el-table :data="plan.phases" border size="small">
            <el-table-column prop="phaseId" label="相位" width="60" />
            <el-table-column prop="direction" label="方向" width="70" />
            <el-table-column label="绿灯" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.greenTime" :min="10" :max="120" size="small" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="黄灯" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.yellowTime" :min="3" :max="10" size="small" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="红灯" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.redTime" :min="10" :max="150" size="small" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="adjustPhase(row)">应用</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="暂无信号方案数据" />
      </div>

      <div class="panel" style="width:300px">
        <div class="panel-header">相位时序</div>
        <div v-if="plan" class="phase-timing">
          <div v-for="phase in plan.phases" :key="phase.phaseId" class="timing-bar-wrap">
            <div class="timing-direction">{{ phase.direction }}</div>
            <div class="timing-bar">
              <span class="timing-seg green" :style="{ width: greenPct(phase) + '%' }">{{ phase.greenTime }}s</span>
              <span class="timing-seg yellow" :style="{ width: yellowPct(phase) + '%' }">{{ phase.yellowTime }}s</span>
              <span class="timing-seg red" :style="{ width: redPct(phase) + '%' }">{{ phase.redTime }}s</span>
            </div>
          </div>
          <div class="timing-cycle">周期: {{ plan.cycleTime || (plan.phases?.reduce((s, p) => s + p.greenTime + p.yellowTime + p.redTime, 0) || 0) }}s</div>
        </div>
        <el-empty v-else description="暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { signalApi, fusionApi } from '@/api'
import { ElMessage } from 'element-plus'

const selectedId = ref('')
const intersections = ref([])
const plan = ref(null)

function greenPct(p) { const t = p.greenTime + p.yellowTime + p.redTime; return t > 0 ? (p.greenTime / t) * 100 : 33 }
function yellowPct(p) { const t = p.greenTime + p.yellowTime + p.redTime; return t > 0 ? (p.yellowTime / t) * 100 : 33 }
function redPct(p) { const t = p.greenTime + p.yellowTime + p.redTime; return t > 0 ? (p.redTime / t) * 100 : 33 }

async function loadIntersections() {
  try {
    const list = await fusionApi.listIntersectionInfo()
    intersections.value = list?.length ? list : []
  } catch (e) { /* fallback */ }
  if (!intersections.value.length) {
    intersections.value = ['INT-001', 'INT-002', 'INT-003'].map(id => ({ intersectionId: id, name: id }))
  }
  if (intersections.value.length > 0 && !selectedId.value) {
    selectedId.value = intersections.value[0].intersectionId
    loadPlan()
  }
}

async function loadPlan() {
  if (!selectedId.value) return
  try {
    plan.value = await signalApi.getPlan(selectedId.value)
  } catch (e) {
    ElMessage.warning('信号控制服务暂不可用')
  }
}

async function adjustPhase(phase) {
  try {
    let traffic = {}
    try {
      const snap = await fusionApi.getSnapshot(selectedId.value)
      if (snap) {
        traffic = {
          intersectionId: selectedId.value,
          flow: snap.flow,
          speed: snap.speed,
          occupancy: snap.occupancy,
          queueLength: snap.queueLength,
          delay: snap.delay
        }
      }
    } catch (_) { /* 仿真接口不可用时使用默认值，DRL会走fallback */ }

    await signalApi.adjustPhase({
      planId: plan.value.planId,
      phaseId: phase.phaseId,
      suggestedGreenTime: phase.greenTime,
      ...traffic
    })
    ElMessage.success(`相位 ${phase.phaseId} 已更新`)
  } catch (e) {
    ElMessage.error('调整失败: ' + e.message)
  }
}

loadIntersections()
</script>

<style scoped>
.signal-control { padding: 4px; height: calc(100vh - 100px); display: flex; flex-direction: column; gap: 4px; }
.toolbar { background: #fff; border: 1px solid #e4e7ed; padding: 8px 12px; display: flex; gap: 8px; align-items: center; }
.content-row { flex: 1; display: flex; gap: 4px; min-height: 0; }
.panel { background: #fff; border: 1px solid #e4e7ed; padding: 12px; overflow-y: auto; }
.panel-header { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5; }

.phase-timing { padding: 4px 0; }
.timing-bar-wrap { margin-bottom: 14px; }
.timing-direction { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 4px; }
.timing-bar { display: flex; height: 24px; border-radius: 3px; overflow: hidden; }
.timing-seg { display: flex; align-items: center; justify-content: center; color: #fff; font-size: 11px; font-weight: 600; }
.timing-seg.green { background: #67c23a; }
.timing-seg.yellow { background: #e6a23c; }
.timing-seg.red { background: #f56c6c; }
.timing-cycle { font-size: 12px; color: #909399; text-align: right; margin-top: 4px; }
</style>
