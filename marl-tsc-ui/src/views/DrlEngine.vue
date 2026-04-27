<template>
  <div class="drl-engine">
    <el-row :gutter="16">
      <el-col :span="6" v-for="agent in agents" :key="agent.intersectionId">
        <el-card shadow="never" class="agent-card" :class="{ training: agent.training }">
          <div class="agent-header">
            <el-tag :type="agent.training ? 'success' : 'info'" size="small" effect="dark">
              {{ agent.training ? '训练中' : '待命' }}
            </el-tag>
            <el-tag type="warning" size="small" v-if="agent.epsilon !== undefined" effect="plain">
              ε={{ agent.epsilon.toFixed(3) }}
            </el-tag>
          </div>
          <div class="agent-title">{{ agent.intersectionId }}</div>
          <div class="agent-metrics">
            <div class="metric">
              <span class="metric-label">状态维度</span>
              <span class="metric-value">{{ agent.stateSize || 8 }}</span>
            </div>
            <div class="metric">
              <span class="metric-label">动作空间</span>
              <span class="metric-value">{{ agent.actionSize || 4 }}</span>
            </div>
            <div class="metric">
              <span class="metric-label">经验池</span>
              <span class="metric-value">{{ agent.replaySize || 0 }}</span>
            </div>
            <div class="metric">
              <span class="metric-label">训练步数</span>
              <span class="metric-value">{{ agent.trainSteps || 0 }}</span>
            </div>
          </div>
          <el-divider style="margin:8px 0" />
          <div class="agent-actions">
            <el-button :type="agent.training ? 'warning' : 'success'" size="small"
              @click="toggleTraining(agent)" style="width:100%">
              {{ agent.training ? '暂停训练' : '开始训练' }}
            </el-button>
            <el-button type="danger" size="small" plain
              @click="resetAgent(agent)" style="width:100%;margin-top:6px">
              重置智能体
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="never" class="section-card">
          <template #header>
            <span style="font-weight:600">训练进度</span>
            <el-button size="small" style="float:right" @click="triggerTrain" :loading="training">
              执行训练步
            </el-button>
          </template>
          <div ref="lossChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="never" class="section-card">
          <template #header><span style="font-weight:600;DRL配置参数"></span></template>
          <el-descriptions :column="4" border size="small">
            <el-descriptions-item label="学习率">{{ config.learningRate }}</el-descriptions-item>
            <el-descriptions-item label="折扣因子 γ">{{ config.gamma }}</el-descriptions-item>
            <el-descriptions-item label="批大小">{{ config.batchSize }}</el-descriptions-item>
            <el-descriptions-item label="经验池容量">{{ config.replayBufferCapacity }}</el-descriptions-item>
            <el-descriptions-item label="目标更新频率">{{ config.targetUpdateFreq }}</el-descriptions-item>
            <el-descriptions-item label="训练频率">{{ config.trainFreq }}</el-descriptions-item>
            <el-descriptions-item label="ε 最小值">{{ config.epsilonMin }}</el-descriptions-item>
            <el-descriptions-item label="ε 衰减">{{ config.epsilonDecay }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { drlApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const agents = ref([])
const training = ref(false)
const lossChartRef = ref(null)
let lossChart = null

const config = {
  learningRate: 0.001, gamma: 0.95, batchSize: 32,
  replayBufferCapacity: 10000, targetUpdateFreq: 100,
  trainFreq: 4, epsilonMin: 0.01, epsilonDecay: 0.995
}

async function loadAgents() {
  try {
    agents.value = await drlApi.listAgents() || []
  } catch (e) {
    console.error(e)
    // Show demo data when backend unavailable
    agents.value = generateDemoAgents()
  }
}

function generateDemoAgents() {
  return ['INT-001', 'INT-002', 'INT-003', 'INT-004'].map(id => ({
    intersectionId: id,
    training: false,
    epsilon: 0.5 + Math.random() * 0.5,
    stateSize: 8,
    actionSize: 4,
    replaySize: Math.floor(Math.random() * 5000),
    trainSteps: Math.floor(Math.random() * 1000)
  }))
}

async function toggleTraining(agent) {
  try {
    await drlApi.toggleTraining(agent.intersectionId)
    agent.training = !agent.training
    ElMessage.success(`${agent.intersectionId}: ${agent.training ? '开始训练' : '暂停训练'}`)
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  }
}

async function resetAgent(agent) {
  try {
    await ElMessageBox.confirm(`确定重置 ${agent.intersectionId} 的智能体?`, '确认')
    await drlApi.resetAgent(agent.intersectionId)
    agent.trainSteps = 0
    agent.replaySize = 0
    agent.epsilon = 1.0
    ElMessage.success('智能体已重置')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('重置失败')
  }
}

async function triggerTrain() {
  training.value = true
  try {
    await drlApi.train({ steps: 1 })
    ElMessage.success('训练步完成')
    loadAgents()
  } catch (e) {
    ElMessage.warning('训练服务暂不可用')
  } finally {
    training.value = false
  }
}

let lossData = Array.from({ length: 20 }, () => Math.random() * 2)

function renderChart() {
  if (!lossChartRef.value) return
  if (!lossChart) lossChart = echarts.init(lossChartRef.value)
  lossChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: lossData.map((_, i) => i + 1) },
    yAxis: { type: 'value', name: 'Loss' },
    series: [{ name: 'Loss', type: 'line', smooth: true, data: lossData, itemStyle: { color: '#f56c6c' } }]
  })
}

onMounted(() => { loadAgents(); nextTick(renderChart) })
onBeforeUnmount(() => lossChart?.dispose())
</script>

<style scoped>
.drl-engine { padding: 4px; }
.agent-card { border-radius: 8px; margin-bottom: 16px; transition: all 0.3s; }
.agent-card.training { border-left: 3px solid #67c23a; }
.agent-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.agent-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; color: #303133; }
.agent-metrics { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.metric { text-align: center; }
.metric-label { display: block; font-size: 12px; color: #909399; }
.metric-value { display: block; font-size: 18px; font-weight: bold; color: #409eff; }
.agent-actions { display: flex; flex-direction: column; gap: 4px; }
.section-card { border-radius: 6px; }
</style>
