<template>
  <div class="signal-control">
    <el-card shadow="never" class="section-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-select v-model="selectedId" placeholder="选择路口" size="large" style="width:100%"
            @change="loadPlan">
            <el-option v-for="item in intersections" :key="item.intersectionId"
              :label="item.name || item.intersectionId" :value="item.intersectionId" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" size="large" @click="loadPlan" :icon="Refresh">刷新</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="16">
        <el-card shadow="never" class="section-card">
          <template #header><span style="font-weight:600">信号相位详情</span></template>
          <div v-if="plan">
            <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
              <el-descriptions-item label="方案ID">{{ plan.planId }}</el-descriptions-item>
              <el-descriptions-item label="周期(秒)">{{ plan.cycleTime }}</el-descriptions-item>
              <el-descriptions-item label="当前相位">{{ plan.currentPhase }}</el-descriptions-item>
            </el-descriptions>
            <el-table :data="plan.phases" border stripe size="small">
              <el-table-column prop="phaseId" label="相位" width="60" />
              <el-table-column prop="direction" label="方向" width="80" />
              <el-table-column label="绿灯(s)" width="100">
                <template #default="{ row }">
                  <el-input-number v-model="row.greenTime" :min="10" :max="120" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="黄灯(s)" width="100">
                <template #default="{ row }">
                  <el-input-number v-model="row.yellowTime" :min="3" :max="10" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="红灯(s)" width="100">
                <template #default="{ row }">
                  <el-input-number v-model="row.redTime" :min="10" :max="150" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="操作">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="adjustPhase(row)">应用</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="暂无信号方案数据" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="section-card">
          <template #header><span style="font-weight:600">相位状态</span></template>
          <div v-if="plan" class="phase-visual">
            <div v-for="phase in plan.phases" :key="phase.phaseId" class="phase-bar">
              <div class="phase-label">{{ phase.direction }}</div>
              <div class="light-bar">
                <span class="light green" :style="{ width: greenPct(phase) + '%' }">{{ phase.greenTime }}s</span>
                <span class="light yellow" :style="{ width: yellowPct(phase) + '%' }">{{ phase.yellowTime }}s</span>
                <span class="light red" :style="{ width: redPct(phase) + '%' }">{{ phase.redTime }}s</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { signalApi } from '@/api'
import { webApi } from '@/api'
import { ElMessage } from 'element-plus'

const selectedId = ref('')
const intersections = ref([])
const plan = ref(null)

function greenPct(p) { const t = p.greenTime + p.yellowTime + p.redTime; return t > 0 ? (p.greenTime / t) * 100 : 33 }
function yellowPct(p) { const t = p.greenTime + p.yellowTime + p.redTime; return t > 0 ? (p.yellowTime / t) * 100 : 33 }
function redPct(p) { const t = p.greenTime + p.yellowTime + p.redTime; return t > 0 ? (p.redTime / t) * 100 : 33 }

async function loadIntersections() {
  try {
    intersections.value = await webApi.listIntersections() || []
    if (intersections.value.length > 0) {
      selectedId.value = intersections.value[0].intersectionId
      loadPlan()
    }
  } catch (e) { console.error(e) }
}

async function loadPlan() {
  if (!selectedId.value) return
  try {
    plan.value = await signalApi.getPlan(selectedId.value)
  } catch (e) {
    console.error(e)
    ElMessage.warning('信号控制服务暂不可用')
  }
}

async function adjustPhase(phase) {
  try {
    await signalApi.adjustPhase({
      planId: plan.value.planId,
      phaseId: phase.phaseId,
      suggestedGreenTime: phase.greenTime
    })
    ElMessage.success(`相位 ${phase.phaseId} 已更新`)
  } catch (e) {
    ElMessage.error('调整失败: ' + e.message)
  }
}

loadIntersections()
</script>

<style scoped>
.signal-control { padding: 4px; }
.section-card { border-radius: 6px; }
.phase-visual { padding: 8px 0; }
.phase-bar { margin-bottom: 16px; }
.phase-label { font-size: 14px; font-weight: 600; margin-bottom: 4px; }
.light-bar { display: flex; height: 28px; border-radius: 4px; overflow: hidden; }
.light { display: flex; align-items: center; justify-content: center; color: #fff; font-size: 12px; font-weight: bold; transition: width 0.3s; }
.light.green { background: #67c23a; }
.light.yellow { background: #e6a23c; }
.light.red { background: #f56c6c; }
</style>
