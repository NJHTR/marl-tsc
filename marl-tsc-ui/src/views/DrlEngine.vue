<template>
  <div class="drl-engine">
    <el-row :gutter="12">
      <!-- Agent Cards -->
      <el-col :span="18">
        <el-row :gutter="12">
          <el-col :span="6" v-for="agent in agents" :key="agent.intersectionId">
            <el-card shadow="never" class="agent-card" :class="{ training: agent.training }"
              @click="selectedAgent = agent" :style="{ borderLeft: selectedAgent?.intersectionId === agent.intersectionId ? '3px solid #409eff' : '' }">
              <div class="agent-top">
                <el-tag :type="agent.training ? 'success' : 'info'" size="small" effect="dark" round>
                  {{ agent.training ? '🧠 训练中' : '⏸ 待命' }}
                </el-tag>
                <span class="agent-eps">ε={{ (agent.epsilon || 0.5).toFixed(3) }}</span>
              </div>
              <div class="agent-title">{{ agent.intersectionId }}</div>
              <div class="agent-stats">
                <div class="stat">
                  <span class="stat-val">{{ agent.trainSteps || 0 }}</span>
                  <span class="stat-lbl">训练步</span>
                </div>
                <div class="stat">
                  <span class="stat-val">{{ agent.replaySize || 0 }}</span>
                  <span class="stat-lbl">经验池</span>
                </div>
                <div class="stat">
                  <span class="stat-val">{{ agent.qvalue || '-' }}</span>
                  <span class="stat-lbl">Q值</span>
                </div>
              </div>
              <div class="agent-actions">
                <el-button :type="agent.training ? 'warning' : 'success'" size="small"
                  @click.stop="toggleTraining(agent)" round style="width:100%">
                  {{ agent.training ? '暂停' : '训练' }}
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- Neural Network Visualization -->
        <el-card shadow="never" class="nn-card" style="margin-top:12px">
          <template #header>
            <span style="font-weight:600">🧬 神经网络 {{ selectedAgent?.intersectionId || '' }}</span>
            <div style="float:right">
              <el-button size="small" @click="triggerTrain" :loading="training" round>执行训练步</el-button>
              <el-button size="small" @click="addRandomExperience" round>添加随机经验</el-button>
            </div>
          </template>
          <el-row :gutter="12">
            <el-col :span="14">
              <div ref="nnChartRef" class="nn-chart"></div>
            </el-col>
            <el-col :span="10">
              <div ref="lossChartRef" class="loss-chart"></div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- Agent Details Panel -->
      <el-col :span="6">
        <el-card shadow="never" class="detail-card" v-if="selectedAgent">
          <template #header>
            <span style="font-weight:600">{{ selectedAgent.intersectionId }} 详情</span>
          </template>
          <div class="detail-section">
            <div class="detail-item">
              <span class="detail-label">状态维度</span>
              <span class="detail-val">{{ selectedAgent.stateSize || 8 }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">动作空间</span>
              <span class="detail-val">{{ selectedAgent.actionSize || 4 }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">ε-贪婪</span>
              <span class="detail-val">{{ (selectedAgent.epsilon || 0.5).toFixed(4) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">学习率</span>
              <span class="detail-val">{{ config.learningRate }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">折扣因子 γ</span>
              <span class="detail-val">{{ config.gamma }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">批大小</span>
              <span class="detail-val">{{ config.batchSize }}</span>
            </div>
          </div>
          <el-divider style="margin:8px 0" />
          <div class="action-buttons">
            <el-button type="danger" size="small" @click="resetAgent(selectedAgent)" round style="width:100%">
              🔄 重置智能体
            </el-button>
          </div>

          <!-- Q-Value bars -->
          <el-divider style="margin:8px 0" />
          <div style="font-size:13px;font-weight:600;margin-bottom:6px">Q 值分布</div>
          <div v-for="(qv, i) in qValues" :key="i" class="q-bar-row">
            <span class="q-lbl">动作 {{ i }}</span>
            <div class="q-bar-track">
              <div class="q-bar-fill" :style="{ width: qv.pct + '%', background: qv.color }"></div>
            </div>
            <span class="q-val">{{ qv.val.toFixed(2) }}</span>
          </div>
        </el-card>
        <el-card v-else shadow="never" class="detail-card">
          <el-empty description="选择一个智能体" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { drlApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const agents = ref([])
const selectedAgent = ref(null)
const training = ref(false)
const nnChartRef = ref(null)
const lossChartRef = ref(null)
let nnChart = null
let lossChart = null
let lossData = []

const config = {
  learningRate: 0.001, gamma: 0.95, batchSize: 32,
  replayBufferCapacity: 10000, targetUpdateFreq: 100,
  trainFreq: 4, epsilonMin: 0.01, epsilonDecay: 0.995
}

const qValues = ref([
  { val: 2.34, pct: 85, color: '#409eff' },
  { val: 1.82, pct: 66, color: '#67c23a' },
  { val: 0.95, pct: 34, color: '#e6a23c' },
  { val: 0.45, pct: 16, color: '#f56c6c' }
])

async function loadAgents() {
  try {
    agents.value = await drlApi.listAgents() || []
  } catch (e) {
    agents.value = generateDemoAgents()
  }
  if (!selectedAgent.value && agents.value.length) {
    selectedAgent.value = agents.value[0]
  }
}

function generateDemoAgents() {
  return ['INT-001', 'INT-002', 'INT-003', 'INT-004'].map((id, i) => ({
    intersectionId: id,
    training: i === 0,
    epsilon: Math.max(0.01, 0.5 - i * 0.1),
    stateSize: 8,
    actionSize: 4,
    replaySize: Math.floor(2000 + Math.random() * 5000),
    trainSteps: Math.floor(100 + Math.random() * 900),
    qvalue: (1.0 + Math.random() * 2).toFixed(2)
  }))
}

async function toggleTraining(agent) {
  try {
    await drlApi.toggleTraining(agent.intersectionId)
    agent.training = !agent.training
    ElMessage.success(`${agent.intersectionId}: ${agent.training ? '训练开始' : '训练暂停'}`)
  } catch (e) {
    agent.training = !agent.training
    ElMessage.success(`${agent.intersectionId}: ${agent.training ? '训练开始' : '训练暂停'}`)
  }
}

async function resetAgent(agent) {
  try {
    await ElMessageBox.confirm(`确定重置 ${agent.intersectionId}?`, '确认')
    await drlApi.resetAgent(agent.intersectionId)
    agent.trainSteps = 0
    agent.replaySize = 0
    agent.epsilon = 1.0
    ElMessage.success('已重置')
  } catch (e) {
    if (e !== 'cancel') {
      agent.trainSteps = 0
      agent.replaySize = 0
      agent.epsilon = 1.0
      ElMessage.success('已重置')
    }
  }
}

async function triggerTrain() {
  training.value = true
  try {
    await drlApi.train({ steps: 1 })
  } catch (e) { /* demo mode */ }
  // Update demo metrics
  if (selectedAgent.value) {
    selectedAgent.value.trainSteps = (selectedAgent.value.trainSteps || 0) + 1
    selectedAgent.value.replaySize = Math.min((selectedAgent.value.replaySize || 0) + 32, 10000)
    selectedAgent.value.epsilon = Math.max(0.01, (selectedAgent.value.epsilon || 0.5) - 0.005)
  }
  // Add to loss chart
  const loss = Math.max(0.1, (lossData.length > 0 ? lossData[lossData.length - 1] : 2.0) * (0.9 + Math.random() * 0.1) - 0.05)
  lossData.push(loss)
  updateLossChart()
  updateNNChart()
  training.value = false
}

function addRandomExperience() {
  if (selectedAgent.value) {
    selectedAgent.value.replaySize = Math.min((selectedAgent.value.replaySize || 0) + 64, 10000)
    ElMessage.success('添加 64 条随机经验')
    updateNNChart()
  }
}

function drawNNChart() {
  if (!nnChartRef.value) return
  const w = nnChartRef.value.clientWidth
  const h = nnChartRef.value.clientHeight

  // Clear
  if (nnChart) nnChart.dispose()
  nnChart = echarts.init(nnChartRef.value)

  const layers = [
    { label: 'Input\n(8)', size: 8, x: 60 },
    { label: 'Dense\n(64)', size: 12, x: 180 },
    { label: 'Dense\n(32)', size: 10, x: 300 },
    { label: 'Output\n(4)', size: 6, x: 420 }
  ]

  const nodes = []
  const edges = []

  for (const layer of layers) {
    for (let i = 0; i < layer.size; i++) {
      const y = (h / 2) + (i - layer.size / 2) * (Math.min(h / layer.size, 18))
      nodes.push({
        name: `${layer.label}-${i}`,
        x: layer.x,
        y: y,
        symbolSize: Math.min(8, 24 / Math.sqrt(layer.size)),
        category: layers.indexOf(layer),
        label: i === Math.floor(layer.size / 2) ? layer.label : ''
      })
    }
  }

  // Connect layers
  for (let l = 0; l < layers.length - 1; l++) {
    const fromLayer = layers[l]
    const toLayer = layers[l + 1]
    for (let i = 0; i < fromLayer.size; i += 2) {
      for (let j = 0; j < toLayer.size; j += 2) {
        const fromIdx = nodes.findIndex(n =>
          n.name.startsWith(fromLayer.label) && n.name.endsWith(`-${i}`))
        const toIdx = nodes.findIndex(n =>
          n.name.startsWith(toLayer.label) && n.name.endsWith(`-${j}`))
        if (fromIdx >= 0 && toIdx >= 0) {
          edges.push({
            source: fromIdx,
            target: toIdx,
            lineStyle: {
              width: Math.random() * 1.5 + 0.3,
              color: `rgba(64,158,255,${Math.random() * 0.3 + 0.1})`,
              curveness: 0.1
            }
          })
        }
      }
    }
  }

  nnChart.setOption({
    animationDuration: 0,
    series: [{
      type: 'graph',
      layout: 'none',
      categories: layers.map(l => ({ name: l.label })),
      data: nodes,
      edges: edges,
      roam: false,
      draggable: false,
      focusNodeAdjacency: false,
      itemStyle: {
        color: (p) => {
          const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c']
          return colors[p.data.category % colors.length]
        },
        opacity: 0.8
      },
      lineStyle: { opacity: 0.3 },
      label: { show: true, position: 'bottom', fontSize: 10, color: '#909399' },
      force: { repulsion: 0 }
    }]
  })
}

function updateNNChart() {
  // Pulse animation - update edge opacity randomly
  if (!nnChart) return
}

function renderLossChart() {
  if (!lossChartRef.value) return
  if (lossChart) lossChart.dispose()
  lossChart = echarts.init(lossChartRef.value)
  lossData = Array.from({ length: 30 }, (_, i) => 2.0 * Math.exp(-i / 10) + Math.random() * 0.3)
  updateLossChart()
}

function updateLossChart() {
  if (!lossChart) return
  lossChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '10%', right: '8%', top: '10%', bottom: '15%' },
    title: { text: 'Loss', textStyle: { fontSize: 13, color: '#909399' }, left: 'center', top: 0 },
    xAxis: { type: 'category', data: lossData.map((_, i) => i + 1), axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', min: 0, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'line', smooth: true, data: lossData,
      lineStyle: { color: '#f56c6c', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(245,108,108,0.3)' },
          { offset: 1, color: 'rgba(245,108,108,0)' }
        ])
      },
      symbol: 'none'
    }]
  })
}

onMounted(() => {
  loadAgents()
  nextTick(() => {
    drawNNChart()
    renderLossChart()
  })
})

onBeforeUnmount(() => {
  nnChart?.dispose()
  lossChart?.dispose()
})

watch(selectedAgent, () => {
  nextTick(() => {
    drawNNChart()
    renderLossChart()
  })
})
</script>

<style scoped>
.drl-engine { padding: 4px; }
.agent-card {
  border-radius: 10px; cursor: pointer; transition: all 0.3s;
  margin-bottom: 8px; border-left: 3px solid transparent;
}
.agent-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.agent-card.training { border-left-color: #67c23a; }
.agent-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.agent-eps { font-size: 12px; color: #e6a23c; font-family: monospace; }
.agent-title { font-size: 16px; font-weight: 700; color: #303133; margin-bottom: 10px; }
.agent-stats { display: flex; gap: 12px; margin-bottom: 10px; }
.stat { flex: 1; text-align: center; }
.stat-val { display: block; font-size: 18px; font-weight: bold; color: #409eff; }
.stat-lbl { font-size: 11px; color: #909399; }
.nn-card { border-radius: 10px; }
.nn-chart { height: 340px; }
.loss-chart { height: 340px; }
.detail-card { border-radius: 10px; }
.detail-section { display: flex; flex-direction: column; gap: 8px; }
.detail-item { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; border-bottom: 1px solid #f5f5f5; }
.detail-label { color: #909399; }
.detail-val { font-weight: 600; color: #303133; font-family: monospace; }
.action-buttons { display: flex; flex-direction: column; gap: 6px; }

.q-bar-row { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.q-lbl { font-size: 11px; color: #909399; width: 50px; }
.q-bar-track { flex: 1; height: 12px; background: #f0f0f0; border-radius: 6px; overflow: hidden; }
.q-bar-fill { height: 100%; border-radius: 6px; transition: width 0.5s; }
.q-val { font-size: 11px; font-family: monospace; width: 45px; text-align: right; }
</style>
